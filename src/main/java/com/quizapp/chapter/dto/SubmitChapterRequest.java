package com.quizapp.chapter.dto;

import java.util.List;

public record SubmitChapterRequest(List<ProgressAnswerDto> answers) {}
