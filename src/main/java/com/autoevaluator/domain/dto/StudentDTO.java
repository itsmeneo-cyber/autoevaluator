package com.autoevaluator.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class StudentDTO {
    private String username;
    private String password;
    private String role;
    private String rollNo;
    private String name;
    private int  semester;
    private String department;
    private String college;
    private List<EnrolmentInfoDTO> enrolments;
    private int year;
}
