package com.quizapp.chapter.dto;

import java.util.List;

public record ProgressRequest(
        int currentIndex, int correctCount, int wrongCount, List<ProgressAnswerDto> answers) {}
