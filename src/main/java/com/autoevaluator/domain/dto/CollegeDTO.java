package com.autoevaluator.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * DTO for creating a College along with its Departments and Courses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollegeDTO {
    private String name;
    private String establishedYear;
    private String type;
    private String location;
    private List<DepartmentDTO> departments;
    // Add this field to pass the admin's username/email
    private String adminUsername;

    private String adminName;
}
