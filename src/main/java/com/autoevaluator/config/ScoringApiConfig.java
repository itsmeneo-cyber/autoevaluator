package com.autoevaluator.config;


import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class ScoringApiConfig {

    @Value("${spring.scoring.api-url}")
    private String scoringApiUrl;

}

