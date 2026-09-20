package com.quizapp.quiz;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, QuizQuestion.Pk> {

    @Query("select qq from QuizQuestion qq where qq.quizId = :quizId order by qq.position")
    List<QuizQuestion> findByQuizIdOrdered(Long quizId);

    boolean existsByQuizIdAndQuestionId(Long quizId, Long questionId);
}
