package com.quizapp.attempt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AnswerItem(
        @NotNull Long questionId,
        @NotBlank String selectedOption,
        @NotNull Integer timeTakenMs) {}
