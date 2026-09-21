package com.quizapp.chapter;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "chapter_questions")
@IdClass(ChapterQuestion.Pk.class)
public class ChapterQuestion {

    @Id
    @Column(name = "chapter_id")
    private Long chapterId;

    @Id
    @Column(name = "question_id")
    private Long questionId;

    @Column(nullable = false)
    private int position;

    public Long getChapterId() { return chapterId; }
    public void setChapterId(Long chapterId) { this.chapterId = chapterId; }
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public static class Pk implements Serializable {
        private Long chapterId;
        private Long questionId;

        public Pk() {}
        public Pk(Long chapterId, Long questionId) {
            this.chapterId = chapterId;
            this.questionId = questionId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk pk)) return false;
            return Objects.equals(chapterId, pk.chapterId) && Objects.equals(questionId, pk.questionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(chapterId, questionId);
        }
    }
}
