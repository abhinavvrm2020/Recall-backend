package com.quizapp.quiz;

import com.quizapp.attempt.UserQuizAttempt;
import com.quizapp.attempt.UserQuizAttemptRepository;
import com.quizapp.common.ApiException;
import com.quizapp.common.util.Futures;
import com.quizapp.common.util.QuestionPayloadMapper;
import com.quizapp.config.VirtualThreadConfig;
import com.quizapp.question.Question;
import com.quizapp.question.QuestionRepository;
import com.quizapp.quiz.dto.QuestionDto;
import com.quizapp.quiz.dto.QuizDetailDto;
import com.quizapp.quiz.dto.QuizDto;
import com.quizapp.quiz.dto.StartAttemptResponse;
import com.quizapp.subject.SubjectService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuestionRepository questionRepository;
    private final UserQuizAttemptRepository attemptRepository;
    private final SubjectService subjectService;
    private final QuestionPayloadMapper questionPayloadMapper;
    private final ExecutorService virtualExecutor;

    public QuizService(
            QuizRepository quizRepository,
            QuizQuestionRepository quizQuestionRepository,
            QuestionRepository questionRepository,
            UserQuizAttemptRepository attemptRepository,
            SubjectService subjectService,
            QuestionPayloadMapper questionPayloadMapper,
            @Qualifier(VirtualThreadConfig.VIRTUAL_EXECUTOR) ExecutorService virtualExecutor) {
        this.quizRepository = quizRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.subjectService = subjectService;
        this.questionPayloadMapper = questionPayloadMapper;
        this.virtualExecutor = virtualExecutor;
    }

    @Transactional(readOnly = true)
    public List<QuizDto> listBySubject(Long subjectId) {
        subjectService.requireById(subjectId);
        return quizRepository.findBySubjectIdAndStatusOrderByTotalQuestionsAsc(subjectId, "ACTIVE").stream()
                .map(this::toQuizDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuizDetailDto getQuizDetail(Long quizId) {
        CompletableFuture<Quiz> quizFut = Futures.supply(() -> requireActiveQuiz(quizId), virtualExecutor);
        CompletableFuture<List<QuizQuestion>> linksFut =
                Futures.supply(() -> quizQuestionRepository.findByQuizIdOrdered(quizId), virtualExecutor);
        Futures.all(quizFut, linksFut).join();

        Quiz quiz = quizFut.join();
        List<Long> questionIds = linksFut.join().stream().map(QuizQuestion::getQuestionId).toList();

        Map<Long, Question> byId = Futures.supply(
                        () -> questionRepository.findAllById(questionIds).stream()
                                .collect(Collectors.toMap(Question::getId, q -> q)),
                        virtualExecutor)
                .join();

        List<CompletableFuture<QuestionDto>> dtoJobs = questionIds.stream()
                .map(qid -> Futures.supply(
                        () -> {
                            Question question = byId.get(qid);
                            return question == null ? null : toQuestionDto(question);
                        },
                        virtualExecutor))
                .toList();

        List<QuestionDto> questions =
                Futures.joinAll(dtoJobs).stream().filter(Objects::nonNull).toList();
        return new QuizDetailDto(quiz.getId(), quiz.getSubjectId(), quiz.getTotalQuestions(), questions);
    }

    @Transactional
    public StartAttemptResponse startAttempt(Long quizId, Long userId) {
        Quiz quiz = requireActiveQuiz(quizId);
        if (!"ACTIVE".equals(quiz.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Quiz not active");
        }
        UserQuizAttempt attempt = new UserQuizAttempt();
        attempt.setUserId(userId);
        attempt.setQuizId(quiz.getId());
        attemptRepository.save(attempt);
        return new StartAttemptResponse(attempt.getId(), quiz.getId());
    }

    public Quiz requireById(Long quizId) {
        return quizRepository
                .findById(quizId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Quiz not found"));
    }

    private Quiz requireActiveQuiz(Long quizId) {
        return requireById(quizId);
    }

    private QuizDto toQuizDto(Quiz quiz) {
        return new QuizDto(
                quiz.getId(), quiz.getSubjectId(), quiz.getTotalQuestions(), quiz.getType(), quiz.getStatus());
    }

    private QuestionDto toQuestionDto(Question question) {
        return new QuestionDto(
                question.getId(),
                questionPayloadMapper.text(question),
                questionPayloadMapper.options(question),
                question.getAllottedTimeMs());
    }
}
