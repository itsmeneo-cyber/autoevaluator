package com.autoevaluator.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class College {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;
    private String establishedYear;
    private String type;
    private String location;

    // A college can have multiple departments
    @OneToMany(mappedBy = "college", cascade = CascadeType.ALL)
    @ToString.Exclude // Avoid recursion in toString method
    private List<Department> departments = new ArrayList<>();

    // A college can have multiple teachers (One-to-Many relationship)
    @OneToMany(mappedBy = "college", cascade = CascadeType.ALL)
    @ToString.Exclude // Avoid recursion in toString method
    private List<Teacher> teachers = new ArrayList<>();
}
