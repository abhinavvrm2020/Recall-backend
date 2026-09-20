package com.quizapp.attempt.dto;

import java.time.Instant;

public record AttemptHistoryDto(
        Long attemptId, Long quizId, Integer totalCorrect, Integer totalQuestions, Instant completedAt) {}
