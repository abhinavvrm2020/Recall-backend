package com.quizapp.chapter.dto;

public record ProgressAnswerDto(Long questionId, String selectedOption, boolean correct, int timeTakenMs) {}
