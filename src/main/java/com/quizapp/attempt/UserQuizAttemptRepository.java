package com.quizapp.attempt;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserQuizAttemptRepository extends JpaRepository<UserQuizAttempt, Long> {
    List<UserQuizAttempt> findByUserIdOrderByStartedAtDesc(Long userId);
}
