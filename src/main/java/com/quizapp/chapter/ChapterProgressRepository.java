package com.quizapp.chapter;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChapterProgressRepository extends JpaRepository<ChapterProgress, Long> {
    Optional<ChapterProgress> findByUserIdAndChapterId(Long userId, Long chapterId);
}
