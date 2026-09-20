package com.quizapp.quiz;

import jakarta.persistence.*;
import lombok.Setter;

@Entity
@Table(name = "quizzes")
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Setter
    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";

    @Setter
    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Setter
    @Column(nullable = false, length = 32)
    private String type = "PRACTICE";

    public Long getId() { return id; }
    public Long getSubjectId() { return subjectId; }

    public String getStatus() { return status; }

    public int getTotalQuestions() { return totalQuestions; }

    public String getType() { return type; }
}
