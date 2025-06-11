package com.autoevaluator.domain.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EvaluationResponseDto {
    private String questionNumber;
    private String teacherAnswer;
    private String studentAnswer;
    private Double marksObtained;
    private Integer totalMarks;
}

