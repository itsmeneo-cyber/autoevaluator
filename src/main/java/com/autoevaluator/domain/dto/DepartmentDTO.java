package com.autoevaluator.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * DTO for Department inside a College request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDTO {
    private String name;
    private List<CourseDTO> courses;
}
