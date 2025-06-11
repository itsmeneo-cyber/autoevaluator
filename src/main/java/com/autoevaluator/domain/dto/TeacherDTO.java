package com.autoevaluator.domain.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

/**
 * Data Transfer Object for Teacher.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherDTO {
    private String name;
    private String registrationId;
    private String username;
    private String password;
    private String role;  // Ideally always "TEACHER"
    private String departmentName; // 👈 updated to single department
    private List<String> courseNames;
}


