package com.autoevaluator.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Inheritance(strategy = InheritanceType.JOINED)
//With Joined Table Inheritance, you'll have separate tables for Student, Teacher, and Admin,
// and the AppUser table will store common fields and reference the other tables through foreign keys.
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // <-- Primary key, auto-increment or sequence-generated

    @Column(unique = true, nullable = false)
    private String username; // email of the user, must be unique

    private String password; // encoded password

    private String role; // admin, student, teacher

    private LocalDateTime createdAt; // user creation time
}

