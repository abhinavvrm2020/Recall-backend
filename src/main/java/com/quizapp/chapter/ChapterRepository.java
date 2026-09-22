package com.quizapp.chapter;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    List<Chapter> findBySubjectIdAndStatusOrderBySortOrderAsc(Long subjectId, String status);

    List<Chapter> findBySubjectId(Long subjectId);

    boolean existsBySubjectIdAndTitle(Long subjectId, String title);

    long countBySubjectIdAndTitleNot(Long subjectId, String title);
}
