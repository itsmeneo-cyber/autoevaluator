package com.autoevaluator.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;


@Entity
@Data
@NoArgsConstructor
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String courseName;
    private String courseCode;
    private String courseCredits;
    private int semester;


    @ManyToMany(mappedBy = "courses")
    @ToString.Exclude
    private List<Teacher> teachers;

    @ManyToOne
    @JoinColumn(name = "department_id")
    @ToString.Exclude
    private Department department;
//This ensures that the foreign key column in the Course table
// is exactly named department_id, and the database schema will reflect that explicitly.
}

