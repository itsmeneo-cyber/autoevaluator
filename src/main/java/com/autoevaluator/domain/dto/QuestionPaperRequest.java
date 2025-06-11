package com.autoevaluator.domain.dto;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

@Data
public class QuestionPaperRequest {
    @Parameter(description = "Question paper for midTerm or EndTerm or assignment")
    private String questionPaperFor;

    @Parameter(description = "Semester of the question paper")
    private int semester;

    @Parameter(description = "College Name")
    private String collegeName;

    @Parameter(description = "Department name")
    private String departmentName;

    @Parameter(description = "Course name for which the question paper is created")
    private String courseName;

    @Parameter(description = "Total Marks")
    private int totalMarks;

    @Parameter(description = "Created by")
    private String username;
    @Parameter(description = "Assignment number (only if paper type is Assignment)")
    private Integer assignmentNumber;



}
