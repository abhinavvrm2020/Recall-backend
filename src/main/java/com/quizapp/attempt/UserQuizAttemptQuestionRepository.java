package com.quizapp.attempt;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserQuizAttemptQuestionRepository extends JpaRepository<UserQuizAttemptQuestion, UserQuizAttemptQuestion.Pk> {
    List<UserQuizAttemptQuestion> findByAttemptId(Long attemptId);
}
