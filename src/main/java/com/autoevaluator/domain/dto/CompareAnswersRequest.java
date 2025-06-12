package com.autoevaluator.domain.dto;

import lombok.Data;

@Data
public class CompareAnswersRequest {
    private String teacher_answer;
    private String student_answer;
    private double total_marks;

    // Getters and setters
}