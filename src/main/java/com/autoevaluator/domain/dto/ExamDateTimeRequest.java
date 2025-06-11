package com.autoevaluator.domain.dto;

import lombok.Data;

@Data
public class ExamDateTimeRequest {
    private String examDate; // e.g., "2025-06-15"
    private String examTime; // e.g., "10:00 AM"


}
