package com.autoevaluator.domain.dto;

import lombok.Data;

@Data
public class QuestionAnswerDTO {
    private String questionText;
    private String correctAnswer;
    private Double marks;
    private Long id;
    private String questionNumber;
    private String instructions;
}
