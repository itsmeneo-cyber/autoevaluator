package com.autoevaluator.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String answerLabel;   // e.g. "Ans1", "Ans2"

    private Double obtainedMarks; // e.g. 15

    private Integer totalMarks;    // e.g. 20

    @Enumerated(EnumType.STRING)
    private AnswerSheetType type;  // MIDTERM, ENDTERM, ASSIGNMENT

    @ManyToOne
    @JoinColumn(name = "enrolment_id")
    private Enrolment enrolment;

    @ManyToOne
    @JoinColumn(name = "assignment_submission_id")
    private AssignmentSubmission assignmentSubmission;

    @Column(columnDefinition = "TEXT")
    private String answerText;   // stores detailed answer text

    private String feedback;
}
