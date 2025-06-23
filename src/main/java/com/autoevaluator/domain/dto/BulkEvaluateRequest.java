package com.autoevaluator.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkEvaluateRequest {
    private String courseName;
    private String evaluationType; // MIDTERM, ENDTERM, ASSIGNMENT
    private Integer assignmentNumber; // Optional unless type == ASSIGNMENT
}
