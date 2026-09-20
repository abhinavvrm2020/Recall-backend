package com.quizapp.revision.dto;

import java.time.Instant;

public record RevisionListItemDto(Long id, Long subjectId, String status, int dueCount, Instant createdAt) {}
