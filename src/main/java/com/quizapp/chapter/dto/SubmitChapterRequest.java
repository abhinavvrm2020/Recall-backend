package com.quizapp.chapter.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SubmitChapterRequest(@NotEmpty List<@Valid ProgressAnswerDto> answers) {}
