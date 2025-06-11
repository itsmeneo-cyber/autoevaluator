package com.autoevaluator.domain.dto;

import lombok.Data;

@Data
public class QuestionResponse {
    private String questionNumber; // "Q1", "Q2", etc.
    private String text;           // The question text
    private Double marks;          // Marks for the question
    private String instructions;   // Instructions for the question (optional)
}
