package com.autoevaluator.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class StudentAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    private Enrolment enrolment;

    @ManyToOne
    private Question question;

    private String submittedAnswer;
    private Double awardedMarks;
}

