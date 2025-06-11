package com.autoevaluator.application;

import org.springframework.stereotype.Component;

@Component
public class AnswerSheetEvaluator {

    public Double evaluate(String answerSheetUrl) {
        // TODO: Replace this mock logic with real evaluation later
        // For now, randomly assign marks between 30-90
        return 30 + Math.random() * 60;
    }
}
