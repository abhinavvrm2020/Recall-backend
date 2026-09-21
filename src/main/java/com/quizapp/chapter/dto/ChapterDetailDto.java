package com.quizapp.chapter.dto;

import com.quizapp.attempt.dto.RevisionSummaryDto;
import com.quizapp.quiz.dto.QuestionDto;
import java.util.List;

public record ChapterDetailDto(
        Long id,
        Long subjectId,
        String title,
        ProgressSnapshotDto progress,
        List<QuestionDto> questions,
        RevisionSummaryDto revision) {}
