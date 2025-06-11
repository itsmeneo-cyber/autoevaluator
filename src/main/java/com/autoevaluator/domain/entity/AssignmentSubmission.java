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
public class AssignmentSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String assignmentNumber;

    private String answerSheetUrl;

    @Column(columnDefinition = "TEXT")
    private String assignmentSheetText;  // updated from @Lob to TEXT

    private Double marks;

    @ManyToOne
    @JoinColumn(name = "enrolment_id", nullable = false)
    private Enrolment enrolment;

    @OneToMany(mappedBy = "assignmentSubmission", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnswerScore> answerScores = new ArrayList<>();
}
