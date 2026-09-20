package com.quizapp.revision.dto;

public record SubmitRevisionResponse(Long revisionId, String status, int remainingDue) {}
