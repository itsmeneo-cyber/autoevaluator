package com.autoevaluator.domain.dto;


import lombok.Data;

@Data
public class CompareAnswersResponse {
    private double score;
    private double entailment;
    private double neutral;
    private double contradiction;

    // Getters and setters
}
