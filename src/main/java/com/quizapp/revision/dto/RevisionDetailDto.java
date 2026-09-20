package com.quizapp.revision.dto;

import java.util.List;

public record RevisionDetailDto(Long id, Long subjectId, String status, List<RevisionQuestionDto> questions) {}
