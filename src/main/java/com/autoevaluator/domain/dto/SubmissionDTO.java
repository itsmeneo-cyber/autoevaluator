package com.autoevaluator.domain.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class SubmissionDTO {
    private Long studentId;
    private Long courseId;
    private String type; // "midterm" or "endterm"
    private MultipartFile file; // File upload (PDF)
    private String description;
}
