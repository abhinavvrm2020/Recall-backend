package com.quizapp.quiz.dto;

import java.util.Map;

public record QuestionDto(
        Long id,
        String question,
        Map<String, String> options,
        int allottedTimeMs,
        String correctOption,
        String explanation) {}
