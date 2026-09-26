package com.quizapp.note.dto;

import java.time.Instant;

public record NoteDto(
        Long id,
        Long questionId,
        String noteDescription,
        Long userId,
        Instant createdAt,
        Instant updatedAt) {}
