package com.autoevaluator.application;

import com.autoevaluator.config.ScoringApiConfig;
import com.autoevaluator.domain.dto.CompareAnswersRequest;
import com.autoevaluator.domain.dto.CompareAnswersResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ScoringService {

    @Autowired
    private ScoringApiConfig scoringApiConfig;

    private final RestTemplate restTemplate = new RestTemplate();

    public Double simulateScoringApiCall(String correctAnswer, String studentAnswer, int questionMarks) {
        if (studentAnswer == null || studentAnswer.trim().isEmpty()) {
            return 0.0;
        }

        CompareAnswersRequest requestBody = new CompareAnswersRequest();
        requestBody.setTeacher_answer(correctAnswer);
        requestBody.setStudent_answer(studentAnswer);
        requestBody.setTotal_marks(questionMarks);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CompareAnswersRequest> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<CompareAnswersResponse> response = restTemplate.exchange(
                scoringApiConfig.getScoringApiUrl(),
                HttpMethod.POST,
                entity,
                CompareAnswersResponse.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Scoring API failed with status: " + response.getStatusCode());
        }

        CompareAnswersResponse body = response.getBody();
        if (body == null) {
            throw new RuntimeException("Scoring API response was empty");
        }

        return body.getScore();
    }
}
