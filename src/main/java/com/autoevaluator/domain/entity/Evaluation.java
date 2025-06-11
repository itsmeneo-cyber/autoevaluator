package com.autoevaluator.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne
    private Student student;

    @ManyToOne
    private Course course;

    private Double marks; // Total marks

    private String grade; // Optional if you want

    private String remarks; // Anything teacher wants to write

    private LocalDateTime evaluatedAt;
}
