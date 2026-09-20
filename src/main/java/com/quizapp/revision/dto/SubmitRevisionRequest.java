package com.quizapp.revision.dto;

import com.quizapp.attempt.dto.AnswerItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SubmitRevisionRequest(@NotEmpty List<@Valid AnswerItem> answers) {}
