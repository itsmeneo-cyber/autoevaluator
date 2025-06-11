package com.autoevaluator.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@PrimaryKeyJoinColumn(name = "id")
public class Teacher extends AppUser {

    private String registrationId;
    private String name;

    // A teacher belongs to one department (ManyToOne relationship)
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "department_id")  // Foreign key for department
    @ToString.Exclude
    private Department department;  // One department per teacher

    // A teacher can be assigned to multiple courses
    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(
            name = "teacher_courses",           // Association table name
            joinColumns = @JoinColumn(name = "teacher_id"),    // Join column for the teacher
            inverseJoinColumns = @JoinColumn(name = "course_id") // Join column for the course
    )
    @ToString.Exclude
    private List<Course> courses;

    // A teacher belongs to one college (ManyToOne relationship)
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "college_id")  // Foreign key for college
    private College college;  // One college per teacher
}
