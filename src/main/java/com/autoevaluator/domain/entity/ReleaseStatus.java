package com.autoevaluator.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReleaseStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    private AnswerSheetType answerSheetType; // MIDTERM, ENDTERM, ASSIGNMENT

    private Integer assignmentNumber; // Nullable for MIDTERM and ENDTERM

    private boolean released;
}
