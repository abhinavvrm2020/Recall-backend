package com.quizapp.attempt.dto;

public record SubmitAttemptResponse(
        Long attemptId, int totalCorrect, int totalQuestions, RevisionSummaryDto revision) {}
