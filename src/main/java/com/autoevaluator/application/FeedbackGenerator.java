package com.autoevaluator.application;

public class FeedbackGenerator {

    public static String generateFeedback(double entailment, double neutral, double contradiction) {
        if (entailment > 0.85) {
            return "Excellent understanding! The student's response captures all key concepts clearly and accurately.";
        } else if (entailment > 0.6 && neutral > 0.2) {
            return "Good effort! The answer reflects understanding but could benefit from deeper explanation or more complete coverage.";
        } else if (neutral > 0.5) {
            return "The response shows some grasp of the topic but lacks completeness. Encourage elaboration and stronger connections to the main idea.";
        } else if (neutral > 0.3 && contradiction > 0.3) {
            return "The answer has some relevance but misses several key points and includes some conflicting information.";
        } else if (contradiction > 0.6) {
            return "The student's answer contradicts the key ideas in the model answer. Suggest reviewing the concepts again.";
        } else if (entailment < 0.3 && neutral < 0.3 && contradiction < 0.3) {
            return "The response appears unclear or off-topic. Recommend revisiting the question and rephrasing the answer.";
        } else {
            return "Mixed response. Some parts are valid, but clarity and completeness need improvement.";
        }
    }
}

