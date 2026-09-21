package com.quizapp.chapter.dto;

public record ProgressSnapshotDto(
        String status,
        int currentIndex,
        int correctCount,
        int wrongCount,
        int totalQuestions,
        Long revisionId) {}
