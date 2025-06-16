package com.autoevaluator.adapter.handler.rest;

import com.autoevaluator.application.CollegeService;
import com.autoevaluator.domain.dto.CollegeDTO;

import com.autoevaluator.domain.dto.CourseDTO;
import com.autoevaluator.domain.dto.DepartmentDTO;
import com.autoevaluator.domain.dto.DepartmentDTOGPT;
import com.autoevaluator.domain.exception.BadRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/college")
public class CollegeController extends BaseRestController {

    private final CollegeService collegeService;

    public CollegeController(CollegeService collegeService) {
        this.collegeService = collegeService;
    }

    @GetMapping("/getAllColleges")
    @Operation(
            summary = "Get all College Details",
            description = "Fetches all Colleges along with their Departments and Courses.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Colleges fetched successfully."),
            @ApiResponse(responseCode = "404", description = "No colleges found.")
    })
    public ResponseEntity<List<CollegeDTO>> getAllColleges() {
        List<CollegeDTO> collegeDTOs = collegeService.getAllColleges();
        return ResponseEntity.ok(collegeDTOs); // Always return the list, regardless of its size
    }

    @GetMapping("/get")
    @Operation(summary = "Get College Details",
            description = "Fetches a College along with its Departments and Courses.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "College fetched successfully."),
            @ApiResponse(responseCode = "404", description = "College not found."),
    })
    public ResponseEntity<CollegeDTO> getCollege(@RequestParam String collegeName) {
        CollegeDTO collegeDTO = collegeService.getCollegeByName(collegeName);
        return ResponseEntity.ok(collegeDTO);
    }


    @Operation(summary = "Update a College",
            description = "Updates college details by college name.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "College updated successfully."),
            @ApiResponse(responseCode = "404", description = "College not found."),
    })
    @PutMapping("/update/{collegeName}")
    public ResponseEntity<String> updateCollege(@PathVariable String collegeName,
                                                @RequestBody CollegeDTO collegeDTO) {
        collegeService.updateCollege(collegeName, collegeDTO);
        return ResponseEntity.ok("College updated successfully.");
    }

    //@PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/deleteCollege")
    @Operation(summary = "Delete a College", description = "Deletes a College by name.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "College deleted successfully."),
            @ApiResponse(responseCode = "404", description = "College not found.")
    })
    public ResponseEntity<String> deleteCollege(@RequestParam String collegeName) {
        boolean deleted = collegeService.deleteCollege(collegeName);
        if (deleted) {
            return ResponseEntity.ok("College deleted successfully.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("College not found.");
        }
    }

    //    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/add", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Add a new College",
            description = "Creates a new College along with its Departments and Courses. Only accessible by users with ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "College created successfully."),
            @ApiResponse(responseCode = "403", description = "Only Admins can perform this operation."),
            @ApiResponse(responseCode = "400", description = "Invalid input data.")
    })
    public ResponseEntity<Map<String, String>> addCollege(@RequestBody CollegeDTO collegeDTO) {
        collegeService.addCollege(collegeDTO);
        Map<String, String> response = new HashMap<>();
        response.put("message", "College added successfully.");
        return ResponseEntity.ok(response);
    }


    // NEW: Endpoint to get full college details
    @GetMapping("/getDepartmentsByCollege/{collegeName}")
    public ResponseEntity<List<DepartmentDTOGPT>> getDepartmentsByCollege(@PathVariable String collegeName) {
        List<DepartmentDTOGPT> departments = collegeService.getDepartmentsByCollege(collegeName);
        return ResponseEntity.ok(departments);
    }

    @PutMapping("/updateDepartment")
    @Operation(summary = "Update a Department in a College",
            description = "Updates Department details by department and college name.")
    public ResponseEntity<String> updateDepartmentInCollege(@RequestParam String collegeName, @RequestParam String departmentName, @RequestBody DepartmentDTO departmentDTO) {
        collegeService.updateDepartmentInCollege(collegeName, departmentName, departmentDTO);
        return ResponseEntity.ok("Department updated successfully.");
    }

    @PostMapping("/deleteDepartment/{collegeName}/{departmentName}")
    @Operation(summary = "Delete a Department from a College",
            description = "Deletes a Department by department and college name.")
    public ResponseEntity<String> deleteDepartmentFromCollege(
            @PathVariable String collegeName,
            @PathVariable String departmentName) {
        collegeService.deleteDepartmentFromCollege(collegeName, departmentName);
        return ResponseEntity.ok("Department deleted successfully.");
    }


    @PostMapping(value = "/addDepartment/{collegeName}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> addDepartmentToCollege(@PathVariable String collegeName, @RequestBody DepartmentDTO departmentDTO) {
        collegeService.addDepartmentToCollege(collegeName, departmentDTO);
        return ResponseEntity.ok("Department added to the college successfully.");
    }



    @GetMapping("/getCoursesFromDepartment/{collegeName}/{departmentName}")
    @Operation(summary = "Get Courses from a Department",
            description = "Fetches all courses from a department under a specific college.")
    public ResponseEntity<List<CourseDTO>> getCoursesFromDepartment(
            @PathVariable String collegeName,
            @PathVariable String departmentName) {
        List<CourseDTO> courses = collegeService.getCoursesFromDepartment(collegeName, departmentName);
        return ResponseEntity.ok(courses);
    }




    @PostMapping(value = "/addCourseToDepartment/{collegeName}/{departmentName}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Add a Course to a Department",
            description = "Creates a new Course and associates it with an existing Department in a specific College. Only accessible by users with ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Course added to the department successfully."),
            @ApiResponse(responseCode = "403", description = "Only Admins can perform this operation."),
            @ApiResponse(responseCode = "400", description = "Invalid input data.")
    })
    public ResponseEntity<String> addCourseToDepartment(@PathVariable String collegeName,
                                                        @PathVariable String departmentName,
                                                        @RequestBody CourseDTO courseDTO) {

            collegeService.addCourseToDepartment(collegeName, departmentName, courseDTO);
            return ResponseEntity.ok("Course added to the department successfully.");

    }



    // Get Course from Department
    @GetMapping("/getCourse")
    @Operation(summary = "Get a Course from a Department",
            description = "Fetches a specific course from a department under a college.")
    public ResponseEntity<CourseDTO> getCourseFromDepartment(
            @RequestParam String collegeName,
            @RequestParam String departmentName,
            @RequestParam String courseName) {
        CourseDTO courseDTO = collegeService.getCourseFromDepartment(collegeName, departmentName, courseName);
        return ResponseEntity.ok(courseDTO);
    }

    // Update Course in Department
    @PutMapping("/updateCourse")
    @Operation(summary = "Update a Course in a Department",
            description = "Updates an existing course inside a department under a college.")
    public ResponseEntity<String> updateCourseInDepartment(
            @RequestParam String collegeName,
            @RequestParam String departmentName,
            @RequestParam String courseName,
            @RequestBody CourseDTO updatedCourseDTO) {
        collegeService.updateCourseInDepartment(collegeName, departmentName, courseName, updatedCourseDTO);
        return ResponseEntity.ok("Course updated successfully.");
    }

    // Delete Course from Department
@PostMapping("/deleteCourse")
    @Operation(summary = "Delete a Course from a Department",
            description = "Deletes a course from a department under a college.")
    public ResponseEntity<String> deleteCourseFromDepartment(
            @RequestParam String collegeName,
            @RequestParam String departmentName,
            @RequestParam String courseName) {
    collegeService.deleteCourseFromDepartment(collegeName, departmentName, courseName);
    return ResponseEntity.ok("Course deleted successfully.");
}
}
