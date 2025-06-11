package com.autoevaluator.domain.dto;

import lombok.Data;

@Data
public class QuestionRequest {
    private String questionNumber;
    private String text;
    private Double marks;
    private String instructions;
    private String correctAnswer;
}


