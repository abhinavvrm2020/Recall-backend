package com.quizapp.chapter.dto;

import java.util.List;

public record SubjectChaptersResponse(
        Long subjectRevisionId, int subjectRevisionDueCount, List<ChapterListItemDto> chapters) {}
