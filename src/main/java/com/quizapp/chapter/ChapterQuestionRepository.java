package com.quizapp.chapter;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChapterQuestionRepository extends JpaRepository<ChapterQuestion, ChapterQuestion.Pk> {
    List<ChapterQuestion> findByChapterIdOrderByPositionAsc(Long chapterId);
    boolean existsByChapterIdAndQuestionId(Long chapterId, Long questionId);
}
