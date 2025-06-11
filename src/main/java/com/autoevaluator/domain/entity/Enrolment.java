package com.autoevaluator.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enrolment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    private String midtermAnswerSheetUrl;

    @Column(columnDefinition = "TEXT")
    private String midtermAnswerSheetText;

    private String endtermAnswerSheetUrl;

    @Column(columnDefinition = "TEXT")
    private String endtermAnswerSheetText;

    private Double midtermMarks;
    private Double endtermMarks;

    @OneToMany(mappedBy = "enrolment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnswerScore> answerScores = new ArrayList<>();

    @OneToMany(mappedBy = "enrolment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AssignmentSubmission> assignments = new ArrayList<>();
}
