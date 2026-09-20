package com.quizapp.quiz.dto;

public record QuizDto(Long id, Long subjectId, int totalQuestions, String type, String status) {}
