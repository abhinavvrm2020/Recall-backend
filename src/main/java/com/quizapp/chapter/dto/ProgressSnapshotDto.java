package com.quizapp.chapter.dto;

import java.util.List;

public record ProgressSnapshotDto(
        String status,
        int currentIndex,
        int correctCount,
        int wrongCount,
        int totalQuestions,
        Long revisionId,
        List<ProgressAnswerDto> answers) {}
