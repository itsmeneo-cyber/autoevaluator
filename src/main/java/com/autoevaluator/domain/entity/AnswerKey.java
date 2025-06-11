package com.autoevaluator.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;;

@Entity
@Data
@NoArgsConstructor
public class AnswerKey {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String correctAnswer;//The subjective answer for the question

    @OneToOne
    private Question question;
}

