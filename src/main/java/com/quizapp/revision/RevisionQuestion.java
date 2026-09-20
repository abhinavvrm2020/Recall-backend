package com.quizapp.revision;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "revision_questions")
public class RevisionQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "revision_id", nullable = false)
    private Long revisionId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(nullable = false, length = 16)
    private String reason;

    @Column(name = "remaining_reviews", nullable = false)
    private int remainingReviews;

    @Column(name = "next_review_at", nullable = false)
    private Instant nextReviewAt;

    public Long getId() { return id; }
    public Long getRevisionId() { return revisionId; }
    public void setRevisionId(Long revisionId) { this.revisionId = revisionId; }
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public int getRemainingReviews() { return remainingReviews; }
    public void setRemainingReviews(int remainingReviews) { this.remainingReviews = remainingReviews; }
    public Instant getNextReviewAt() { return nextReviewAt; }
    public void setNextReviewAt(Instant nextReviewAt) { this.nextReviewAt = nextReviewAt; }
}
