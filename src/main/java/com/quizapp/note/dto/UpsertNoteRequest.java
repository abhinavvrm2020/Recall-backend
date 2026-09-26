package com.quizapp.note.dto;

import jakarta.validation.constraints.NotBlank;

public record UpsertNoteRequest(@NotBlank String noteDescription) {}
