package com.quizapp.revision.dto;

import java.util.Map;

public record RevisionQuestionDto(
        Long revisionQuestionId,
        Long questionId,
        String question,
        Map<String, String> options,
        int allottedTimeMs,
        String correctOption,
        String explanation,
        String moreInformation,
        String reason,
        int remainingReviews) {}
