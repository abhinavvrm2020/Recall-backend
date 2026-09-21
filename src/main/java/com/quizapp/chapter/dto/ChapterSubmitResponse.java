package com.quizapp.chapter.dto;

import com.quizapp.attempt.dto.RevisionSummaryDto;

public record ChapterSubmitResponse(
        int correctCount,
        int wrongCount,
        int totalQuestions,
        int preparednessPct,
        RevisionSummaryDto revision) {}
