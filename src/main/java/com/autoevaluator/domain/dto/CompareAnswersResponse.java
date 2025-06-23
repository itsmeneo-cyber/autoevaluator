package com.autoevaluator.domain.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompareAnswersResponse {
    private double score;
    private double entailment;
    private double neutral;
    private double contradiction;



    // Getters and setters
}
