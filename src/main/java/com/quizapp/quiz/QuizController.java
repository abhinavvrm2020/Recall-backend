package com.quizapp.quiz;

import com.quizapp.common.util.AuthContext;
import com.quizapp.quiz.dto.QuizDetailDto;
import com.quizapp.quiz.dto.QuizDto;
import com.quizapp.quiz.dto.StartAttemptResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping("/subjects/{id}/quizzes")
    public List<QuizDto> quizzesForSubject(@PathVariable Long id) {
        return quizService.listBySubject(id);
    }

    @GetMapping("/quizzes/{id}")
    public QuizDetailDto getQuiz(@PathVariable Long id) {
        return quizService.getQuizDetail(id);
    }

    @PostMapping("/quizzes/{id}/attempts")
    public StartAttemptResponse startAttempt(@PathVariable Long id) {
        return quizService.startAttempt(id, AuthContext.currentUserId());
    }
}
