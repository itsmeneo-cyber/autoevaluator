package com.autoevaluator.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDTOGPT {
    private String name;
    private long totalCourses;
    private long totalTeachers;

}
