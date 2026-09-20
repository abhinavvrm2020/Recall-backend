package com.quizapp.quiz.dto;

import java.util.List;

public record QuizDetailDto(Long id, Long subjectId, int totalQuestions, List<QuestionDto> questions) {}
