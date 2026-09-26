package com.quizapp.revision;

import com.quizapp.attempt.dto.AnswerItem;
import com.quizapp.attempt.dto.CheckAnswerRequest;
import com.quizapp.attempt.dto.CheckAnswerResponse;
import com.quizapp.chapter.ChapterQuestionRepository;
import com.quizapp.common.ApiException;
import com.quizapp.common.util.Futures;
import com.quizapp.common.util.QuestionPayloadMapper;
import com.quizapp.config.VirtualThreadConfig;
import com.quizapp.question.Question;
import com.quizapp.question.QuestionRepository;
import com.quizapp.revision.dto.RevisionDetailDto;
import com.quizapp.revision.dto.RevisionListItemDto;
import com.quizapp.revision.dto.RevisionQuestionDto;
import com.quizapp.revision.dto.SubmitRevisionResponse;
import com.quizapp.user.AuthService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RevisionService {

    private final RevisionRepository revisionRepository;
    private final RevisionQuestionRepository revisionQuestionRepository;
    private final QuestionRepository questionRepository;
    private final ChapterQuestionRepository chapterQuestionRepository;
    private final RevisionAlgorithm revisionAlgorithm;
    private final QuestionPayloadMapper questionPayloadMapper;
    private final AuthService authService;
    private final ExecutorService virtualExecutor;

    public RevisionService(
            RevisionRepository revisionRepository,
            RevisionQuestionRepository revisionQuestionRepository,
            QuestionRepository questionRepository,
            ChapterQuestionRepository chapterQuestionRepository,
            RevisionAlgorithm revisionAlgorithm,
            QuestionPayloadMapper questionPayloadMapper,
            AuthService authService,
            @Qualifier(VirtualThreadConfig.VIRTUAL_EXECUTOR) ExecutorService virtualExecutor) {
        this.revisionRepository = revisionRepository;
        this.revisionQuestionRepository = revisionQuestionRepository;
        this.questionRepository = questionRepository;
        this.chapterQuestionRepository = chapterQuestionRepository;
        this.revisionAlgorithm = revisionAlgorithm;
        this.questionPayloadMapper = questionPayloadMapper;
        this.authService = authService;
        this.virtualExecutor = virtualExecutor;
    }

    @Transactional(readOnly = true)
    public CheckAnswerResponse check(Long revisionId, Long userId, CheckAnswerRequest request) {
        requireOwned(revisionId, userId);
        RevisionQuestion link = revisionQuestionRepository
                .findByRevisionIdAndQuestionId(revisionId, request.questionId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Question not in this revision"));
        Question question = questionRepository
                .findById(link.getQuestionId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Question not found"));
        boolean correct = questionPayloadMapper.isCorrect(question, request.selectedOption());
        return new CheckAnswerResponse(correct, question.getCorrectOption());
    }

    @Transactional(readOnly = true)
    public List<RevisionListItemDto> listOpen(Long userId) {
        Instant now = Instant.now();
        List<Revision> open = revisionRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(
                userId, List.of("PENDING", "IN_PROGRESS"));

        return Futures.joinAll(open.stream()
                .map(revision -> Futures.supply(
                        () -> new RevisionListItemDto(
                                revision.getId(),
                                revision.getSubjectId(),
                                revision.getStatus(),
                                revisionQuestionRepository.findDue(revision.getId(), now).size(),
                                revision.getCreatedAt()),
                        virtualExecutor))
                .toList());
    }

    @Transactional
    public RevisionDetailDto getDueQuestions(Long revisionId, Long userId, Long chapterId) {
        Revision revision = requireOwned(revisionId, userId);
        Instant now = Instant.now();
        List<RevisionQuestion> due = revisionQuestionRepository.findDue(revisionId, now);
        if (chapterId != null) {
            Set<Long> chapterQuestionIds = chapterQuestionRepository
                    .findByChapterIdOrderByPositionAsc(chapterId)
                    .stream()
                    .map(cq -> cq.getQuestionId())
                    .collect(Collectors.toSet());
            due = due.stream()
                    .filter(rq -> chapterQuestionIds.contains(rq.getQuestionId()))
                    .toList();
        }
        if (!due.isEmpty() && "PENDING".equals(revision.getStatus())) {
            revision.setStatus("IN_PROGRESS");
            revisionRepository.save(revision);
        }

        List<Long> questionIds = due.stream().map(RevisionQuestion::getQuestionId).toList();
        Map<Long, Question> questionsById = Futures.supply(
                        () -> questionRepository.findAllById(questionIds).stream()
                                .collect(Collectors.toMap(Question::getId, Function.identity())),
                        virtualExecutor)
                .join();

        List<RevisionQuestionDto> questions = Futures.joinAll(due.stream()
                        .map(rq -> Futures.supply(
                                () -> {
                                    Question question = questionsById.get(rq.getQuestionId());
                                    return question == null ? null : toDto(rq, question);
                                },
                                virtualExecutor))
                        .toList())
                .stream()
                .filter(Objects::nonNull)
                .toList();

        return new RevisionDetailDto(revision.getId(), revision.getSubjectId(), revision.getStatus(), questions);
    }

    @Transactional
    public SubmitRevisionResponse submit(Long revisionId, Long userId, List<AnswerItem> answers) {
        Revision revision = requireOwned(revisionId, userId);

        CompletableFuture<Map<Long, RevisionQuestion>> revisionQuestionsFut = Futures.supply(
                () -> revisionQuestionRepository.findByRevisionId(revisionId).stream()
                        .collect(Collectors.toMap(RevisionQuestion::getQuestionId, Function.identity())),
                virtualExecutor);
        CompletableFuture<Map<Long, Question>> questionsFut = Futures.supply(
                () -> questionRepository
                        .findAllById(answers.stream().map(AnswerItem::questionId).toList())
                        .stream()
                        .collect(Collectors.toMap(Question::getId, Function.identity())),
                virtualExecutor);
        Futures.all(revisionQuestionsFut, questionsFut).join();

        Map<Long, RevisionQuestion> revisionQuestionsById = revisionQuestionsFut.join();
        Map<Long, Question> questionsById = questionsFut.join();

        Futures.joinAll(answers.stream()
                .map(item -> Futures.run(
                        () -> applyInMemory(item, revisionQuestionsById, questionsById), virtualExecutor))
                .toList());

        for (AnswerItem item : answers) {
            RevisionQuestion revisionQuestion = revisionQuestionsById.get(item.questionId());
            if (revisionQuestion != null) {
                revisionQuestionRepository.save(revisionQuestion);
            }
        }

        long stillOpen = revisionQuestionRepository.countByRevisionIdAndRemainingReviewsGreaterThan(revisionId, 0);
        if (stillOpen == 0) {
            revision.setStatus("COMPLETED");
            revisionRepository.save(revision);
        }
        authService.recordActivity(userId);
        int due = revisionQuestionRepository.findDue(revisionId, Instant.now()).size();
        return new SubmitRevisionResponse(revisionId, revision.getStatus(), due);
    }

    private void applyInMemory(
            AnswerItem item,
            Map<Long, RevisionQuestion> revisionQuestionsById,
            Map<Long, Question> questionsById) {
        RevisionQuestion revisionQuestion = revisionQuestionsById.get(item.questionId());
        Question question = questionsById.get(item.questionId());
        if (revisionQuestion == null || question == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Question not in revision: " + item.questionId());
        }
        boolean correct = questionPayloadMapper.isCorrect(question, item.selectedOption());
        revisionAlgorithm.applyRevisionOutcome(revisionQuestion, correct, item.confidence());
    }

    private Revision requireOwned(Long revisionId, Long userId) {
        Revision revision = revisionRepository
                .findById(revisionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Revision not found"));
        if (!revision.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your revision");
        }
        return revision;
    }

    private RevisionQuestionDto toDto(RevisionQuestion revisionQuestion, Question question) {
        return new RevisionQuestionDto(
                revisionQuestion.getId(),
                question.getId(),
                questionPayloadMapper.text(question),
                questionPayloadMapper.options(question),
                question.getAllottedTimeMs(),
                question.getCorrectOption(),
                questionPayloadMapper.explanation(question),
                question.getMoreInformation(),
                revisionQuestion.getReason(),
                revisionQuestion.getRemainingReviews());
    }
}
