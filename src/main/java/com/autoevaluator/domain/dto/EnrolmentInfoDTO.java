package com.autoevaluator.domain.dto;

import lombok.Data;

@Data
public class EnrolmentInfoDTO {
    private String courseName;
    private String courseCode;
    private String midtermAnswerSheetUrl;
    private String endtermAnswerSheetUrl;
    private Double midtermMarks;
    private Double endtermMarks;
    private String teacherName;  // The assigned teacher
}
