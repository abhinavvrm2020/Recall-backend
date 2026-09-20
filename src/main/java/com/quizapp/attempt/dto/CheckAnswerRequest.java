package com.quizapp.attempt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CheckAnswerRequest(
        @NotNull Long questionId,
        @NotBlank String selectedOption) {}
