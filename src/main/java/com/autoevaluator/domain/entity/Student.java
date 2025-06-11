package com.autoevaluator.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@PrimaryKeyJoinColumn(name = "id")
public class Student extends AppUser {
    private String rollNo;
    private String name;
    private int semester;
    @ManyToOne
    @JoinColumn(name = "department_id") // FK column in the Student table
    @ToString.Exclude
    private Department department;

    private String departmentName;
    @ManyToOne
    @JoinColumn(name = "college_id") // Foreign key column in the Student table
    @ToString.Exclude
    private College college;

    // Enrolments (a student can belong to multiple courses)
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Enrolment> enrolments;
}
