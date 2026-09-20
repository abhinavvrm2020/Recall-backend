package com.quizapp.attempt;

import com.quizapp.attempt.dto.AttemptHistoryDto;
import com.quizapp.attempt.dto.CheckAnswerRequest;
import com.quizapp.attempt.dto.CheckAnswerResponse;
import com.quizapp.attempt.dto.SubmitAttemptRequest;
import com.quizapp.attempt.dto.SubmitAttemptResponse;
import com.quizapp.common.util.AuthContext;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AttemptController {

    private final AttemptService attemptService;

    public AttemptController(AttemptService attemptService) {
        this.attemptService = attemptService;
    }

    @PostMapping("/attempts/{id}/check")
    public CheckAnswerResponse check(
            @PathVariable Long id, @Valid @RequestBody CheckAnswerRequest request) {
        return attemptService.check(id, AuthContext.currentUserId(), request);
    }

    @PostMapping("/attempts/{id}/submit")
    public SubmitAttemptResponse submit(
            @PathVariable Long id, @Valid @RequestBody SubmitAttemptRequest request) {
        return attemptService.submit(id, AuthContext.currentUserId(), request.answers());
    }

    @GetMapping("/me/attempts")
    public List<AttemptHistoryDto> history() {
        return attemptService.history(AuthContext.currentUserId());
    }
}
