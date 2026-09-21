package com.quizapp.chapter.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ProgressRequest(
        int currentIndex,
        int correctCount,
        int wrongCount,
        @NotNull List<@Valid ProgressAnswerDto> answers) {}
