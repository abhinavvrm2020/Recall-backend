package com.quizapp.chapter;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    List<Chapter> findBySubjectIdAndStatusOrderBySortOrderAsc(Long subjectId, String status);
    boolean existsBySubjectIdAndTitle(Long subjectId, String title);
}
