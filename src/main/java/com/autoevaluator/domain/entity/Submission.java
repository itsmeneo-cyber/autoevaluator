package com.autoevaluator.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne
    private Student student; // Direct student instead of enrolment

    @ManyToOne
    private Course course; // Which course this submission is for

    private String type; // "midterm" or "endterm"

    private String fileName;

    private String fileUrl;

    private String description;

    private LocalDateTime uploadedAt;
}
