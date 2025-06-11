package com.autoevaluator.domain.dto;

import lombok.Data;

@Data
public class QuestionDTO {
    private Long id;
    private String text;
    private String type;
    private Double marks;
}
