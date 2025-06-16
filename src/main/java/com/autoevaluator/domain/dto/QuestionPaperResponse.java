package com.autoevaluator.domain.dto;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class QuestionPaperResponse {
    @Parameter(description = "ID of the question paper")
    private Long id;



    @Parameter(description = "Semester of the question paper")
    private int semester;

    @Parameter(description = "Department name")
    private String departmentName;

    @Parameter(description = "college name")
    private String collegeName;

    @Parameter(description = "Course name for which the question paper is created")
    private String courseName;

    @Parameter(description = "Name of the user who created the question paper")
    private String creator;

    @Parameter(description = "List of questions with their correct answers")
    private List<QuestionAnswerDTO> questions;

    @Parameter(description = "Last modified at")
    private LocalDateTime lastModifiedAt;

    @Parameter(description = "midTerm or endTerm")
    private String type;

    @Parameter(description = "AssignmentNumber")
    private Integer assignmentNumber;

    @Parameter(description = "Total marks")
    private Integer totalMarks;

    @Parameter(description = "instructions")
    private String instructions;

    @Parameter(description = "course code")
    private String courseCode;

    @Parameter(description = "marks set so far")
    private Double marksSoFar;


}
