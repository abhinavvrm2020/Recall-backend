package com.quizapp.attempt;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "user_quiz_attempt_questions")
@IdClass(UserQuizAttemptQuestion.Pk.class)
public class UserQuizAttemptQuestion {

    @Id
    @Column(name = "attempt_id")
    private Long attemptId;

    @Id
    @Column(name = "question_id")
    private Long questionId;

    @Column(nullable = false)
    private boolean correct;

    @Column(name = "time_taken_ms", nullable = false)
    private int timeTakenMs;

    public Long getAttemptId() { return attemptId; }
    public void setAttemptId(Long attemptId) { this.attemptId = attemptId; }
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }
    public int getTimeTakenMs() { return timeTakenMs; }
    public void setTimeTakenMs(int timeTakenMs) { this.timeTakenMs = timeTakenMs; }

    public static class Pk implements Serializable {
        private Long attemptId;
        private Long questionId;

        public Pk() {}
        public Pk(Long attemptId, Long questionId) {
            this.attemptId = attemptId;
            this.questionId = questionId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk pk)) return false;
            return Objects.equals(attemptId, pk.attemptId) && Objects.equals(questionId, pk.questionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(attemptId, questionId);
        }
    }
}
