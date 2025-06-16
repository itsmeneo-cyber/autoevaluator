package com.autoevaluator.domain.dto;

import lombok.Data;

@Data
public class ReleaseRequest {
    private String courseCode;
    private String type;
    private boolean status;
    private String assignmentNo; // optional, for assignment case
}
