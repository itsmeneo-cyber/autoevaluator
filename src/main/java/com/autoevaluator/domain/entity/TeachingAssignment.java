package com.autoevaluator.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class TeachingAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne
    private Teacher teacher;

    @ManyToOne
    private Course course;

    @ManyToOne
    private Department department;
//
//    @OneToMany(mappedBy = "teachingAssignment")
//    private List<Enrolment> enrollments;
}
