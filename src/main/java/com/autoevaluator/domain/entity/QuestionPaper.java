package com.autoevaluator.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class QuestionPaper {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String collegeName;
    private int semester;
    private String departmentName;

    private int totalMarks;

    @ManyToOne(fetch = FetchType.LAZY) // <-- Add Lazy
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY) // <-- Add Lazy
    @ToString.Exclude
    private AppUser createdBy;

    @ManyToMany(fetch = FetchType.LAZY) // <-- Add Lazy
    @JoinTable(
            name = "question_paper_shared_with",
            joinColumns = @JoinColumn(name = "question_paper_id"),
            inverseJoinColumns = @JoinColumn(name = "teacher_id")
    )
    private List<AppUser> sharedWith;

    @OneToMany(mappedBy = "questionPaper", cascade = CascadeType.ALL, fetch = FetchType.LAZY) // Explicit lazy
    private List<Question> questions;

    private String createdAt;

    private String courseName;

    @Column(nullable = false)
    private boolean isMidterm = false;

    @Column(nullable = false)
    private boolean isEndterm = false;

    // New fields for assignment support

    @Column(nullable = true)
    private Boolean isAssignment = false;

    @Column(nullable = true)
    private Integer assignmentNumber;

}
