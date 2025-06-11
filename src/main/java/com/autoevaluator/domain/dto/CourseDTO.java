package com.autoevaluator.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {
    private String courseName;
    private String courseCode;
    private String courseCredits;
    private int semester;
    private String departmentName;  // ✅ new field
    private String collegeName;     // ✅ new field
}
