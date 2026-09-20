package com.quizapp.quiz;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findBySubjectIdAndStatusOrderByTotalQuestionsAsc(Long subjectId, String status);
    boolean existsBySubjectIdAndTotalQuestionsAndType(Long subjectId, int totalQuestions, String type);
}
