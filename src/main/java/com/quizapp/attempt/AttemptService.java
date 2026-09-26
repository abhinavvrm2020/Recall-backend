package com.quizapp.attempt;

import com.quizapp.attempt.dto.AnswerItem;
import com.quizapp.attempt.dto.AttemptHistoryDto;
import com.quizapp.attempt.dto.CheckAnswerRequest;
import com.quizapp.attempt.dto.CheckAnswerResponse;
import com.quizapp.attempt.dto.RevisionSummaryDto;
import com.quizapp.attempt.dto.SubmitAttemptResponse;
import com.quizapp.common.ApiException;
import com.quizapp.common.util.Futures;
import com.quizapp.common.util.QuestionPayloadMapper;
import com.quizapp.config.VirtualThreadConfig;
import com.quizapp.question.Question;
import com.quizapp.question.QuestionRepository;
import com.quizapp.quiz.Quiz;
import com.quizapp.quiz.QuizQuestionRepository;
import com.quizapp.quiz.QuizService;
import com.quizapp.revision.Revision;
import com.quizapp.revision.RevisionAlgorithm;
import com.quizapp.revision.RevisionQuestion;
import com.quizapp.revision.RevisionQuestionRepository;
import com.quizapp.revision.RevisionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
public class AttemptService {

    private final UserQuizAttemptRepository attemptRepository;
    private final UserQuizAttemptQuestionRepository attemptQuestionRepository;
    private final QuestionRepository questionRepository;
    private final QuizService quizService;
    private final QuizQuestionRepository quizQuestionRepository;
    private final RevisionRepository revisionRepository;
    private final RevisionQuestionRepository revisionQuestionRepository;
    private final RevisionAlgorithm revisionAlgorithm;
    private final QuestionPayloadMapper questionPayloadMapper;
    private final ExecutorService virtualExecutor;

    public AttemptService(
            UserQuizAttemptRepository attemptRepository,
            UserQuizAttemptQuestionRepository attemptQuestionRepository,
            QuestionRepository questionRepository,
            QuizService quizService,
            QuizQuestionRepository quizQuestionRepository,
            RevisionRepository revisionRepository,
            RevisionQuestionRepository revisionQuestionRepository,
            RevisionAlgorithm revisionAlgorithm,
            QuestionPayloadMapper questionPayloadMapper,
            @Qualifier(VirtualThreadConfig.VIRTUAL_EXECUTOR) ExecutorService virtualExecutor) {
        this.attemptRepository = attemptRepository;
        this.attemptQuestionRepository = attemptQuestionRepository;
        this.questionRepository = questionRepository;
        this.quizService = quizService;
        this.quizQuestionRepository = quizQuestionRepository;
        this.revisionRepository = revisionRepository;
        this.revisionQuestionRepository = revisionQuestionRepository;
        this.revisionAlgorithm = revisionAlgorithm;
        this.questionPayloadMapper = questionPayloadMapper;
        this.virtualExecutor = virtualExecutor;
    }

    @Transactional(readOnly = true)
    public CheckAnswerResponse check(Long attemptId, Long userId, CheckAnswerRequest request) {
        UserQuizAttempt attempt = requireOwnedOpenAttempt(attemptId, userId);
        if (!quizQuestionRepository.existsByQuizIdAndQuestionId(attempt.getQuizId(), request.questionId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Question not in this quiz");
        }
        Question question = questionRepository
                .findById(request.questionId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Question not found"));
        boolean correct = questionPayloadMapper.isCorrect(question, request.selectedOption());
        return new CheckAnswerResponse(correct, question.getCorrectOption());
    }

    @Transactional
    public SubmitAttemptResponse submit(Long attemptId, Long userId, List<AnswerItem> answers) {
        UserQuizAttempt attempt = requireOwnedOpenAttempt(attemptId, userId);
        Set<Long> questionIds = answers.stream().map(AnswerItem::questionId).collect(Collectors.toSet());

        CompletableFuture<Quiz> quizFut =
                Futures.supply(() -> quizService.requireById(attempt.getQuizId()), virtualExecutor);
        CompletableFuture<Map<Long, Question>> questionsFut = Futures.supply(
                () -> questionRepository.findAllById(questionIds).stream()
                        .collect(Collectors.toMap(Question::getId, Function.identity())),
                virtualExecutor);
        Futures.all(quizFut, questionsFut).join();
        Quiz quiz = quizFut.join();
        Map<Long, Question> questionsById = questionsFut.join();

        List<RevisionAlgorithm.ScoredAnswer> scored = Futures.joinAll(answers.stream()
                .map(item -> Futures.supply(() -> score(item, questionsById), virtualExecutor))
                .toList());

        List<UserQuizAttemptQuestion> rows = new java.util.ArrayList<>();
        for (int i = 0; i < answers.size(); i++) {
            rows.add(toRow(attempt.getId(), answers.get(i), scored.get(i).correct()));
        }
        int correctCount = (int) scored.stream().filter(RevisionAlgorithm.ScoredAnswer::correct).count();

        attemptQuestionRepository.saveAll(rows);
        attempt.setTotalCorrect(correctCount);
        attempt.setTotalQuestions(rows.size());
        attempt.setCompletedAt(Instant.now());
        attemptRepository.save(attempt);

        List<RevisionAlgorithm.Candidate> candidates = revisionAlgorithm.candidates(scored, questionsById);
        if (candidates.isEmpty()) {
            return new SubmitAttemptResponse(attempt.getId(), correctCount, rows.size(), null);
        }

        Revision revision = upsertPendingRevision(userId, quiz.getSubjectId(), attempt.getId());
        for (RevisionAlgorithm.Candidate candidate : candidates) {
            mergeCandidate(revision.getId(), candidate);
        }
        int count = revisionQuestionRepository.findByRevisionId(revision.getId()).size();
        return new SubmitAttemptResponse(
                attempt.getId(), correctCount, rows.size(), new RevisionSummaryDto(revision.getId(), count));
    }

    @Transactional(readOnly = true)
    public List<AttemptHistoryDto> history(Long userId) {
        return attemptRepository.findByUserIdOrderByStartedAtDesc(userId).stream()
                .map(a -> new AttemptHistoryDto(
                        a.getId(), a.getQuizId(), a.getTotalCorrect(), a.getTotalQuestions(), a.getCompletedAt()))
                .toList();
    }

    private UserQuizAttempt requireOwnedOpenAttempt(Long attemptId, Long userId) {
        UserQuizAttempt attempt = attemptRepository
                .findById(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));
        if (!attempt.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your attempt");
        }
        if (attempt.getCompletedAt() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Attempt already submitted");
        }
        return attempt;
    }

    private RevisionAlgorithm.ScoredAnswer score(AnswerItem item, Map<Long, Question> questionsById) {
        Question question = questionsById.get(item.questionId());
        if (question == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown question " + item.questionId());
        }
        boolean correct = questionPayloadMapper.isCorrect(question, item.selectedOption());
        return new RevisionAlgorithm.ScoredAnswer(question.getId(), correct, item.confidence());
    }

    private static UserQuizAttemptQuestion toRow(Long attemptId, AnswerItem item, boolean correct) {
        UserQuizAttemptQuestion row = new UserQuizAttemptQuestion();
        row.setAttemptId(attemptId);
        row.setQuestionId(item.questionId());
        row.setCorrect(correct);
        row.setTimeTakenMs(Math.max(0, item.timeTakenMs()));
        return row;
    }

    private Revision upsertPendingRevision(Long userId, Long subjectId, Long attemptId) {
        return revisionRepository
                .findFirstByUserIdAndSubjectIdAndStatus(userId, subjectId, "PENDING")
                .map(existing -> {
                    existing.setSourceAttemptId(attemptId);
                    return revisionRepository.save(existing);
                })
                .orElseGet(() -> {
                    Revision created = new Revision();
                    created.setUserId(userId);
                    created.setSubjectId(subjectId);
                    created.setSourceAttemptId(attemptId);
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
