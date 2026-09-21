package com.quizapp.chapter;

import jakarta.persistence.*;
import lombok.Setter;

@Entity
@Table(name = "chapters")
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Setter
    @Column(nullable = false, length = 200)
    private String title;

    @Setter
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Setter
    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";

    public Long getId() { return id; }
    public Long getSubjectId() { return subjectId; }
    public String getTitle() { return title; }
    public int getSortOrder() { return sortOrder; }
    public String getStatus() { return status; }
}
