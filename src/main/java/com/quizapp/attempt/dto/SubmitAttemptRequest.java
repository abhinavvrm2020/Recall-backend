package com.quizapp.attempt.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SubmitAttemptRequest(@NotEmpty List<@Valid AnswerItem> answers) {}
