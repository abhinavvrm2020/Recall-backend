package com.quizapp.question;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_json", nullable = false, columnDefinition = "jsonb")
    private String questionJson;

    @Column(name = "correct_option", nullable = false, length = 8)
    private String correctOption;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "allotted_time_ms", nullable = false)
    private int allottedTimeMs = 60_000;

    @Column(name = "more_information")
    private String moreInformation;

    public Long getId() { return id; }
    public String getQuestionJson() { return questionJson; }
    public void setQuestionJson(String questionJson) { this.questionJson = questionJson; }
    public String getCorrectOption() { return correctOption; }
    public void setCorrectOption(String correctOption) { this.correctOption = correctOption; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    public int getAllottedTimeMs() { return allottedTimeMs; }
    public void setAllottedTimeMs(int allottedTimeMs) { this.allottedTimeMs = allottedTimeMs; }
    public String getMoreInformation() { return moreInformation; }
    public void setMoreInformation(String moreInformation) { this.moreInformation = moreInformation; }
}
