package com.quizapp.chapter.dto;

public record ChapterListItemDto(
        Long id,
        String title,
        int totalQuestions,
        String progressStatus,
        int currentIndex,
        int correctCount,
        int wrongCount) {}
