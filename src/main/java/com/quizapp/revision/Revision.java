package com.quizapp.revision;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "revisions")
public class Revision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "source_attempt_id")
    private Long sourceAttemptId;

    @Column(nullable = false, length = 32)
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    public Long getSourceAttemptId() { return sourceAttemptId; }
    public void setSourceAttemptId(Long sourceAttemptId) { this.sourceAttemptId = sourceAttemptId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
}
