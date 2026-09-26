package com.quizapp.chapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quizapp.attempt.dto.RevisionSummaryDto;
import com.quizapp.chapter.dto.ChapterDetailDto;
import com.quizapp.chapter.dto.ChapterListItemDto;
import com.quizapp.chapter.dto.ChapterSubmitResponse;
import com.quizapp.chapter.dto.ProgressAnswerDto;
import com.quizapp.chapter.dto.ProgressRequest;
import com.quizapp.chapter.dto.ProgressSnapshotDto;
import com.quizapp.chapter.dto.SubjectChaptersResponse;
import com.quizapp.chapter.dto.SubmitChapterRequest;
import com.quizapp.common.ApiException;
import com.quizapp.common.util.QuestionPayloadMapper;
import com.quizapp.question.Question;
import com.quizapp.question.QuestionRepository;
import com.quizapp.quiz.dto.QuestionDto;
import com.quizapp.revision.Revision;
import com.quizapp.revision.RevisionAlgorithm;
import com.quizapp.revision.RevisionQuestion;
import com.quizapp.revision.RevisionQuestionRepository;
import com.quizapp.revision.RevisionRepository;
import com.quizapp.subject.SubjectService;
import com.quizapp.user.AuthService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChapterService {

    private static final List<String> OPEN_REVISION_STATUSES = List.of("PENDING", "IN_PROGRESS");

    private final ChapterRepository chapterRepository;
    private final ChapterQuestionRepository chapterQuestionRepository;
    private final ChapterProgressRepository progressRepository;
    private final QuestionRepository questionRepository;
    private final QuestionPayloadMapper questionPayloadMapper;
    private final RevisionAlgorithm revisionAlgorithm;
    private final RevisionRepository revisionRepository;
    private final RevisionQuestionRepository revisionQuestionRepository;
    private final SubjectService subjectService;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public ChapterService(
            ChapterRepository chapterRepository,
            ChapterQuestionRepository chapterQuestionRepository,
            ChapterProgressRepository progressRepository,
            QuestionRepository questionRepository,
            QuestionPayloadMapper questionPayloadMapper,
            RevisionAlgorithm revisionAlgorithm,
            RevisionRepository revisionRepository,
            RevisionQuestionRepository revisionQuestionRepository,
            SubjectService subjectService,
            AuthService authService,
            ObjectMapper objectMapper) {
        this.chapterRepository = chapterRepository;
        this.chapterQuestionRepository = chapterQuestionRepository;
        this.progressRepository = progressRepository;
        this.questionRepository = questionRepository;
        this.questionPayloadMapper = questionPayloadMapper;
        this.revisionAlgorithm = revisionAlgorithm;
        this.revisionRepository = revisionRepository;
        this.revisionQuestionRepository = revisionQuestionRepository;
        this.subjectService = subjectService;
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public SubjectChaptersResponse listBySubject(Long subjectId, Long userId) {
        subjectService.requireById(subjectId);
        Instant now = Instant.now();
        Optional<Revision> openRevision = revisionRepository.findFirstByUserIdAndSubjectIdAndStatusIn(
                userId, subjectId, OPEN_REVISION_STATUSES);
        Long subjectRevisionId = openRevision.map(Revision::getId).orElse(null);
        Set<Long> dueQuestionIds = openRevision
                .map(revision -> revisionQuestionRepository.findDue(revision.getId(), now).stream()
                        .map(RevisionQuestion::getQuestionId)
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
        int subjectRevisionDueCount = dueQuestionIds.size();

        List<ChapterListItemDto> chapters = chapterRepository
                .findBySubjectIdAndStatusOrderBySortOrderAsc(subjectId, "ACTIVE")
                .stream()
                .map(chapter -> {
                    List<ChapterQuestion> chapterQuestions =
                            chapterQuestionRepository.findByChapterIdOrderByPositionAsc(chapter.getId());
                    int total = chapterQuestions.size();
                    ChapterProgress progress =
                            progressRepository.findByUserIdAndChapterId(userId, chapter.getId()).orElse(null);
                    int chapterDueCount = 0;
                    if (!dueQuestionIds.isEmpty()) {
                        for (ChapterQuestion chapterQuestion : chapterQuestions) {
                            if (dueQuestionIds.contains(chapterQuestion.getQuestionId())) {
                                chapterDueCount++;
                            }
                        }
                    }
                    Long chapterRevisionId = chapterDueCount > 0 ? subjectRevisionId : null;
                    return new ChapterListItemDto(
                            chapter.getId(),
                            chapter.getTitle(),
                            total,
                            progress == null ? "NOT_STARTED" : progress.getStatus(),
                            progress == null ? 0 : progress.getCurrentIndex(),
                            progress == null ? 0 : progress.getCorrectCount(),
                            progress == null ? 0 : progress.getWrongCount(),
                            chapterDueCount,
                            chapterRevisionId);
                })
                .toList();
        return new SubjectChaptersResponse(subjectRevisionId, subjectRevisionDueCount, chapters);
    }

    @Transactional(readOnly = true)
    public ChapterDetailDto get(Long chapterId, Long userId) {
        Chapter chapter = requireChapter(chapterId);
        List<ChapterQuestion> chapterQuestions =
                chapterQuestionRepository.findByChapterIdOrderByPositionAsc(chapterId);
        List<Long> questionIds = chapterQuestions.stream().map(ChapterQuestion::getQuestionId).toList();
        Map<Long, Question> questionsById = questionRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        List<QuestionDto> questions = questionIds.stream()
                .map(id -> toQuestionDto(requireQuestion(questionsById, id)))
                .toList();
        ChapterProgress progress = progressRepository.findByUserIdAndChapterId(userId, chapterId).orElse(null);
        ProgressSnapshotDto snapshot = snapshot(progress, questions.size());
        RevisionSummaryDto revision = revisionSummary(progress);
        return new ChapterDetailDto(
                chapter.getId(), chapter.getSubjectId(), chapter.getTitle(), snapshot, questions, revision);
    }

    @Transactional
    public void start(Long chapterId, Long userId, boolean restart) {
        requireChapter(chapterId);
        ChapterProgress progress =
                progressRepository.findByUserIdAndChapterId(userId, chapterId).orElse(null);
        if (progress == null) {
            progress = new ChapterProgress();
            progress.setUserId(userId);
            progress.setChapterId(chapterId);
            reset(progress);
            progressRepository.save(progress);
            return;
        }
        if (!restart) {
            if ("COMPLETED".equals(progress.getStatus())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Chapter already completed");
            }
            return;
        }
        reset(progress);
        progressRepository.save(progress);
    }

    @Transactional
    public void saveProgress(Long chapterId, Long userId, ProgressRequest req) {
        requireChapter(chapterId);
        ChapterProgress progress = requireInProgress(chapterId, userId);
        progress.setCurrentIndex(req.currentIndex());
        progress.setCorrectCount(req.correctCount());
        progress.setWrongCount(req.wrongCount());
        progress.setAnswersJson(writeAnswers(req.answers()));
        progress.setLastActiveAt(Instant.now());
        progressRepository.save(progress);
        authService.recordActivity(userId);
    }

    @Transactional
    public ChapterSubmitResponse submit(Long chapterId, Long userId, SubmitChapterRequest req) {
        Chapter chapter = requireChapter(chapterId);
        ChapterProgress progress = requireInProgress(chapterId, userId);
        List<ProgressAnswerDto> answers = req.answers();
        List<Long> expectedQuestionIds = chapterQuestionRepository
                .findByChapterIdOrderByPositionAsc(chapterId)
                .stream()
                .map(ChapterQuestion::getQuestionId)
                .toList();
        validateCompleteAnswers(answers, expectedQuestionIds);
        List<Long> questionIds = answers.stream().map(ProgressAnswerDto::questionId).toList();

        Map<Long, Question> questionsById = questionRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(
                        Question::getId, Function.identity(), (first, ignored) -> first, LinkedHashMap::new));
        List<RevisionAlgorithm.ScoredAnswer> scored = answers.stream()
                .map(answer -> score(answer, questionsById))
                .toList();
        List<ProgressAnswerDto> storedAnswers = answers.stream()
                .map(answer -> {
                    Question question = requireQuestion(questionsById, answer.questionId());
                    return new ProgressAnswerDto(
                            answer.questionId(),
                            answer.selectedOption(),
                            questionPayloadMapper.isCorrect(question, answer.selectedOption()),
                            Math.max(0, answer.timeTakenMs()),
                            answer.confidence());
                })
                .toList();

        int total = scored.size();
        int correctCount = (int) scored.stream().filter(RevisionAlgorithm.ScoredAnswer::correct).count();
        int wrongCount = total - correctCount;
        progress.setStatus("COMPLETED");
        progress.setCurrentIndex(total);
        progress.setCorrectCount(correctCount);
        progress.setWrongCount(wrongCount);
        progress.setAnswersJson(writeAnswers(storedAnswers));
        progress.setLastActiveAt(Instant.now());
        progress.setCompletedAt(Instant.now());

        RevisionSummaryDto revisionSummary = null;
        Revision revision = upsertPendingRevision(userId, chapter.getSubjectId());
        for (RevisionAlgorithm.ScoredAnswer answer : scored) {
            if (answer.correct()
                    && RevisionAlgorithm.CONFIDENCE_SURE.equals(
                            RevisionAlgorithm.normalizeConfidence(answer.confidence()))) {
                graduateFromRevision(revision.getId(), answer.questionId());
            }
        }
        List<RevisionAlgorithm.Candidate> candidates = revisionAlgorithm.candidates(scored, questionsById);
        for (RevisionAlgorithm.Candidate candidate : candidates) {
            mergeCandidate(revision.getId(), candidate);
        }
        long activeCount =
                revisionQuestionRepository.countByRevisionIdAndRemainingReviewsGreaterThan(
                        revision.getId(), 0);
        if (activeCount > 0) {
            progress.setRevisionId(revision.getId());
            revisionSummary = new RevisionSummaryDto(revision.getId(), (int) activeCount);
        } else {
            progress.setRevisionId(null);
        }
        progressRepository.save(progress);
        authService.recordActivity(userId);

        int preparednessPct = total > 0 ? (int) Math.round(100.0 * correctCount / total) : 0;
        return new ChapterSubmitResponse(
                correctCount, wrongCount, total, preparednessPct, revisionSummary);
    }

    private void validateCompleteAnswers(
            List<ProgressAnswerDto> answers, List<Long> expectedQuestionIds) {
        if (answers == null || answers.isEmpty()) {
            throw incompleteAnswers();
        }
        List<Long> submittedQuestionIds =
                answers.stream().map(ProgressAnswerDto::questionId).toList();
        Set<Long> submittedUniqueIds = new HashSet<>(submittedQuestionIds);
        Set<Long> expectedUniqueIds = new HashSet<>(expectedQuestionIds);
        if (submittedQuestionIds.size() != expectedQuestionIds.size()
                || submittedUniqueIds.size() != submittedQuestionIds.size()
                || !submittedUniqueIds.equals(expectedUniqueIds)) {
            throw incompleteAnswers();
        }
    }

    private ApiException incompleteAnswers() {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "Answers must contain every chapter question exactly once");
    }

    private Chapter requireChapter(Long chapterId) {
        return chapterRepository
                .findById(chapterId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chapter not found"));
    }

    private ChapterProgress requireInProgress(Long chapterId, Long userId) {
        ChapterProgress progress = progressRepository
                .findByUserIdAndChapterId(userId, chapterId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Chapter not started"));
        if (!"IN_PROGRESS".equals(progress.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Chapter is not in progress");
        }
        return progress;
    }

    private Question requireQuestion(Map<Long, Question> questionsById, Long questionId) {
        Question question = questionsById.get(questionId);
        if (question == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown question " + questionId);
        }
        return question;
    }

    private RevisionAlgorithm.ScoredAnswer score(
            ProgressAnswerDto answer, Map<Long, Question> questionsById) {
        Question question = requireQuestion(questionsById, answer.questionId());
        boolean correct = questionPayloadMapper.isCorrect(question, answer.selectedOption());
        return new RevisionAlgorithm.ScoredAnswer(question.getId(), correct, answer.confidence());
    }

    private QuestionDto toQuestionDto(Question question) {
        return new QuestionDto(
                question.getId(),
                questionPayloadMapper.text(question),
                questionPayloadMapper.options(question),
                question.getAllottedTimeMs(),
                question.getCorrectOption(),
                questionPayloadMapper.explanation(question),
                question.getMoreInformation());
    }

    private ProgressSnapshotDto snapshot(ChapterProgress progress, int totalQuestions) {
        if (progress == null) {
            return new ProgressSnapshotDto("NOT_STARTED", 0, 0, 0, totalQuestions, null, List.of());
        }
        return new ProgressSnapshotDto(
                progress.getStatus(),
                progress.getCurrentIndex(),
                progress.getCorrectCount(),
                progress.getWrongCount(),
                totalQuestions,
                progress.getRevisionId(),
                readAnswers(progress.getAnswersJson()));
    }

    private RevisionSummaryDto revisionSummary(ChapterProgress progress) {
        if (progress == null
                || !"COMPLETED".equals(progress.getStatus())
                || progress.getRevisionId() == null) {
            return null;
        }
        int count = revisionQuestionRepository.findByRevisionId(progress.getRevisionId()).size();
        return new RevisionSummaryDto(progress.getRevisionId(), count);
    }

    private void reset(ChapterProgress progress) {
        progress.setStatus("IN_PROGRESS");
        progress.setCurrentIndex(0);
        progress.setCorrectCount(0);
        progress.setWrongCount(0);
        progress.setAnswersJson("[]");
        progress.setRevisionId(null);
        progress.setCompletedAt(null);
        progress.setLastActiveAt(Instant.now());
    }

    private String writeAnswers(List<ProgressAnswerDto> answers) {
        try {
            return objectMapper.writeValueAsString(answers);
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to serialize chapter answers");
        }
    }

    private List<ProgressAnswerDto> readAnswers(String answersJson) {
        if (answersJson == null || answersJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(answersJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to parse chapter answers");
        }
    }

    private void graduateFromRevision(Long revisionId, Long questionId) {
        revisionQuestionRepository
                .findByRevisionIdAndQuestionId(revisionId, questionId)
                .ifPresent(existing -> {
                    existing.setRemainingReviews(0);
                    existing.setNextReviewAt(Instant.now().plus(30, ChronoUnit.DAYS));
                    revisionQuestionRepository.save(existing);
                });
    }

    private Revision upsertPendingRevision(Long userId, Long subjectId) {
        return revisionRepository
                .findFirstByUserIdAndSubjectIdAndStatus(userId, subjectId, "PENDING")
                .orElseGet(() -> {
                    Revision created = new Revision();
                    created.setUserId(userId);
                    created.setSubjectId(subjectId);
                    created.setStatus("PENDING");
                    return revisionRepository.save(created);
                });
    }

    private void mergeCandidate(Long revisionId, RevisionAlgorithm.Candidate candidate) {
        RevisionQuestion existing = revisionQuestionRepository
                .findByRevisionIdAndQuestionId(revisionId, candidate.questionId())
                .orElse(null);
        if (existing == null) {
            RevisionQuestion created = new RevisionQuestion();
            created.setRevisionId(revisionId);
            created.setQuestionId(candidate.questionId());
            created.setReason(candidate.reason());
            created.setRemainingReviews(candidate.remainingReviews());
            created.setNextReviewAt(candidate.nextReviewAt());
            revisionQuestionRepository.save(created);
            return;
        }
        if (RevisionAlgorithm.shouldReplaceCandidate(
                existing.getReason(),
                existing.getRemainingReviews(),
                candidate.reason(),
                candidate.remainingReviews())) {
            existing.setReason(candidate.reason());
            existing.setRemainingReviews(candidate.remainingReviews());
            existing.setNextReviewAt(candidate.nextReviewAt());
            revisionQuestionRepository.save(existing);
        }
    }
}
