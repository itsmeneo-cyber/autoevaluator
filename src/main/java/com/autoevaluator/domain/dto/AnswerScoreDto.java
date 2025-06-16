package com.autoevaluator.domain.dto;

import com.autoevaluator.domain.entity.AnswerSheetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class AnswerScoreDto {
    private String rollno;
    private String name;
    private String answerLabel;
    private Double obtainedMarks;
    private Integer totalMarks;
    private String answerText;
    private AnswerSheetType type;
}
