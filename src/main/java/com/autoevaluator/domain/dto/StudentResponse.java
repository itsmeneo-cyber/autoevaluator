package com.autoevaluator.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class StudentResponse {
    private Long id;
    private String rollno;
    private String name;
    private String username;

    // Midterm and Endterm info
    private String midtermAnswerSheetUrl;
    private String endtermAnswerSheetUrl;
    private Double midtermMarks;
    private Double endtermMarks;




    private List<AssignmentData> assignments;

    @Data
    public static class AssignmentData {
        private String assignmentTitle;
        private String answerSheetUrl;
        private Double marks;
    }
}
