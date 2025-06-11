package com.autoevaluator.domain.dto;

import lombok.Data;

@Data
public class ContactRequest {
    private String name;
    private String email;
    private String subject;
    private String message;

    // Getters and setters
}
