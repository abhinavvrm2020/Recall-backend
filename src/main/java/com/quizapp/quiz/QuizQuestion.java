package com.quizapp.quiz;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "quiz_questions")
@IdClass(QuizQuestion.Pk.class)
public class QuizQuestion {

    @Id
    @Column(name = "quiz_id")
    private Long quizId;

    @Id
    @Column(name = "question_id")
    private Long questionId;

    @Column(nullable = false)
    private int position;

    public Long getQuizId() { return quizId; }
    public void setQuizId(Long quizId) { this.quizId = quizId; }
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public static class Pk implements Serializable {
        private Long quizId;
        private Long questionId;

        public Pk() {}
        public Pk(Long quizId, Long questionId) {
            this.quizId = quizId;
            this.questionId = questionId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk pk)) return false;
            return Objects.equals(quizId, pk.quizId) && Objects.equals(questionId, pk.questionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(quizId, questionId);
        }
    }
}
