package com.autoevaluator.adapter.handler.rest;

import com.autoevaluator.application.AdminService;
import com.autoevaluator.application.EmailService;
import com.autoevaluator.application.TeacherService;
import com.autoevaluator.domain.dto.*;
import com.autoevaluator.domain.entity.AppUser;
import com.autoevaluator.domain.entity.Student;
import com.autoevaluator.domain.entity.Teacher;
import com.autoevaluator.domain.exception.BadRequestException;
import com.autoevaluator.domain.models.UserPrincipal;
import com.autoevaluator.domain.repositories.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminController extends BaseRestController {

    private final AdminService adminService;


    @Autowired
    private EmailService emailService;

    @Autowired
    private TeacherService teacherService;


    public AdminController(AdminService adminService) {

        this.adminService = adminService;

    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @PostMapping(value = "/addStudent", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> addStudent(@RequestBody StudentDTO studentDTO) {
        adminService.addStudent(studentDTO);
        return ResponseEntity.ok("Student added successfully.");
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @PostMapping(value = "/addTeacher", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> addTeacher(@RequestParam String collegeName, @RequestParam String departmentName, @RequestBody TeacherDTO teacherDTO) {
        adminService.addTeacher(teacherDTO, collegeName, departmentName);
        return ResponseEntity.ok("Teacher added successfully.");
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @PostMapping(value = "/addAdmin", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> register(@RequestBody AdminDTO adminDTO) {
        adminService.addAdmin(adminDTO);
        return ResponseEntity.ok("Admin added successfully.");
    }


    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @PostMapping("/deleteStudent/{username}")
    public ResponseEntity<String> deleteStudent(@PathVariable String username) {
        adminService.removeStudentByUsername(username);
        return ResponseEntity.ok("Student removed successfully.");
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @PostMapping("/removeTeacher")
    public ResponseEntity<String> removeTeacher(@RequestParam String username, @RequestParam String collegeName, @RequestParam String departmentName) {
        adminService.removeTeacherByUsername(username, collegeName, departmentName);
        return ResponseEntity.ok("Teacher removed successfully.");
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @PostMapping("/removeAdmin/{username}")
    public ResponseEntity<String> removeAdmin(@PathVariable String username) {
        adminService.removeAdminByUsername(username);
        return ResponseEntity.ok("Admin removed successfully.");
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @PutMapping(value = "/updateStudent/{username}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateStudent(@PathVariable String username, @RequestBody StudentDTO studentDTO) {
        adminService.updateStudentByUsername(username, studentDTO);
        return ResponseEntity.ok("Student updated successfully.");
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @PutMapping(value = "/updateTeacher")
    public ResponseEntity<String> updateTeacher(@RequestParam String teacherUsername, @RequestBody TeacherDTO teacherDTO, @RequestParam(required = false) String collegeName, @RequestParam(required = false) String departmentName
    ) {
        adminService.updateTeacherByUsername(teacherUsername, teacherDTO, collegeName, departmentName);
        return ResponseEntity.ok("Teacher updated successfully.");
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @PutMapping(value = "/updateAdmin/{username}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateAdmin(@PathVariable String username, @RequestBody AdminDTO adminDTO) {
        adminService.updateAdminByUsername(username, adminDTO);
        return ResponseEntity.ok("Admin updated successfully.");
    }

    @GetMapping("/getAdmins")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<List<AdminDTO>> getAllAdmins() {
        List<AdminDTO> admins = adminService.getAllAdmins();
        return ResponseEntity.ok(admins);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @GetMapping("/getStudent/{username}")
    public ResponseEntity<StudentDTO> getStudent(@PathVariable String username) {
        StudentDTO studentDTO = adminService.getStudentByUsername(username);
        return ResponseEntity.ok(studentDTO);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @GetMapping("/getStudentsByYearAndSemester/{year}/{semester}")
    public ResponseEntity<?> getStudentsByYearAndSemester(@PathVariable int year, @PathVariable int semester, @RequestParam String collegeName, @RequestParam String departmentName) {
        List<StudentDTO> students = adminService.getStudentsBySemesterAndCollegeAndDepartment(semester, collegeName, departmentName);
        if (students == null || students.isEmpty()) {
            return ResponseEntity
                    .ok(Collections.emptyList());
        }
        return ResponseEntity.ok(students);

    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @GetMapping("/getTeacher")
    public ResponseEntity<TeacherDTO> getTeacher(@RequestParam String username) {
        TeacherDTO teacherDTO = adminService.getTeacherByUsername(username);
        return ResponseEntity.ok(teacherDTO);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @GetMapping("/getTeachers")
    public ResponseEntity<List<TeacherDTO>> getTeachers(@RequestParam String collegeName, @RequestParam String departmentName) {
        List<TeacherDTO> teacherList = adminService.getTeachers(collegeName, departmentName);
        return ResponseEntity.ok(teacherList);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @PostMapping("/assignTeacherToCourse")
    public ResponseEntity<String> assignTeacherToCourse(@RequestParam String teacherUsername, @RequestParam String courseName) {
        adminService.assignTeacherToCourse(teacherUsername, courseName);
        return ResponseEntity.ok("Course assigned successfully");
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @PostMapping(value = "/unassignTeacherFromCourse")
    public ResponseEntity<String> unassignTeacherFromCourse(@RequestParam String teacherUsername, @RequestParam String courseName) {
        adminService.unassignTeacherFromCourse(teacherUsername, courseName);
        return ResponseEntity.ok("Teacher unassigned from course successfully.");
    }

    @PreAuthorize("hasRole('SUPERADMIN')")
    @PostMapping(value = "/addCollege", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> addCollege(@RequestBody CollegeDTO collegeDTO) {
        adminService.addCollege(collegeDTO);
        Map<String, String> response = new HashMap<>();
        response.put("message", "College added successfully.");
        return ResponseEntity.ok(response);
    }



    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @PostMapping(value = "/addDepartment/{collegeName}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> addDepartmentToCollege(@PathVariable String collegeName, @RequestBody DepartmentDTO departmentDTO) {
        adminService.addDepartmentToCollege(collegeName, departmentDTO);
        return ResponseEntity.ok("Department added to the college successfully.");
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @PostMapping(value = "/addCourseToDepartment/{collegeName}/{departmentName}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> addCourseToDepartment(@PathVariable String collegeName, @PathVariable String departmentName, @RequestBody CourseDTO courseDTO) {
        adminService.addCourseToDepartment(collegeName, departmentName, courseDTO);
        return ResponseEntity.ok("Course added to the department successfully.");
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @PutMapping("/updateCollege/{collegeName}")
    public ResponseEntity<String> updateCollege(@PathVariable String collegeName,
                                                @RequestBody CollegeDTO collegeDTO) {
        adminService.updateCollege(collegeName, collegeDTO);
        return ResponseEntity.ok("College updated successfully.");
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @PutMapping("/updateDepartment")
    public ResponseEntity<String> updateDepartmentInCollege(@RequestParam String collegeName, @RequestParam String departmentName, @RequestBody DepartmentDTO departmentDTO) {
        adminService.updateDepartmentInCollege(collegeName, departmentName, departmentDTO);
        return ResponseEntity.ok("Department updated successfully.");
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @PutMapping("/updateCourse")
    public ResponseEntity<String> updateCourseInDepartment(@RequestParam String collegeName, @RequestParam String departmentName, @RequestParam String courseName, @RequestBody CourseDTO updatedCourseDTO) {
        adminService.updateCourseInDepartment(collegeName, departmentName, courseName, updatedCourseDTO);
        return ResponseEntity.ok("Course updated successfully.");
    }


    @GetMapping("/getAllColleges")
    public ResponseEntity<List<CollegeDTO>> getAllColleges() {
        List<CollegeDTO> collegeDTOs = adminService.getAllColleges();
        return ResponseEntity.ok(collegeDTOs); // Always return the list, regardless of its size
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get")
    @Operation(summary = "Get College Details",
            description = "Fetches a College along with its Departments and Courses.")
    public ResponseEntity<CollegeDTO> getCollege(@RequestParam String collegeName) {
        CollegeDTO collegeDTO = adminService.getCollegeByName(collegeName);
        return ResponseEntity.ok(collegeDTO);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @GetMapping("/getDepartmentsByCollege/{collegeName}")
    public ResponseEntity<List<DepartmentDTOGPT>> getDepartmentsByCollege(@PathVariable String collegeName) {
        List<DepartmentDTOGPT> departments = adminService.getDepartmentsByCollege(collegeName);
        return ResponseEntity.ok(departments);
    }


    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @GetMapping("/getCoursesFromDepartment/{collegeName}/{departmentName}")
    public ResponseEntity<List<CourseDTO>> getCoursesFromDepartment(@PathVariable String collegeName, @PathVariable String departmentName) {
        List<CourseDTO> courses = adminService.getCoursesFromDepartment(collegeName, departmentName);
        return ResponseEntity.ok(courses);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @GetMapping("/getCourse")
    @Operation(summary = "Get a Course from a Department",
            description = "Fetches a specific course from a department under a college.")
    public ResponseEntity<CourseDTO> getCourseFromDepartment(@RequestParam String collegeName, @RequestParam String departmentName, @RequestParam String courseName) {
        CourseDTO courseDTO = adminService.getCourseFromDepartment(collegeName, departmentName, courseName);
        return ResponseEntity.ok(courseDTO);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @PostMapping("/deleteCourse")
    @Operation(summary = "Delete a Course from a Department",
            description = "Deletes a course from a department under a college.")
    public ResponseEntity<String> deleteCourseFromDepartment(
            @RequestParam String collegeName,
            @RequestParam String departmentName,
            @RequestParam String courseName) {
        adminService.deleteCourseFromDepartment(collegeName, departmentName, courseName);
        return ResponseEntity.ok("Course deleted successfully.");
    }

    @PreAuthorize("hasRole('SUPERADMIN')")
    @PostMapping("/deleteCollege")
    public ResponseEntity<String> deleteCollege(@RequestParam String collegeName) {
        boolean deleted = adminService.deleteCollege(collegeName);
        if (deleted) {
            return ResponseEntity.ok("College deleted successfully.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("College not found.");
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @PostMapping("/deleteDepartment/{collegeName}/{departmentName}")
    @Operation(summary = "Delete a Department from a College",
            description = "Deletes a Department by department and college name.")
    public ResponseEntity<String> deleteDepartmentFromCollege(@PathVariable String collegeName, @PathVariable String departmentName) {
        adminService.deleteDepartmentFromCollege(collegeName, departmentName);
        return ResponseEntity.ok("Department deleted successfully.");
    }


    @PostMapping("/send-password")
    public ResponseEntity<?> sendOneTimePassword(@RequestParam String username) {
        if (username == null || username.isEmpty()) {
            throw new BadRequestException("Username can't be empty");
        }

        adminService.sendOneTimePassword(username.toLowerCase());

        return ResponseEntity.ok("Password sent successfully");
    }

    @PostMapping("/get-in-touch")
    public ResponseEntity<String> getInTouch(@RequestBody ContactRequest request) {
        if (request.getEmail() == null || request.getMessage() == null) {
            return ResponseEntity.badRequest().body("Email and message are required.");
        }

        String subject = "New Contact Form Submission: " + request.getSubject();
        String body = "Name: " + request.getName() + "\n"
                + "Email: " + request.getEmail() + "\n\n"
                + "Message:\n" + request.getMessage();

        // Send the message to Faizan's email
        emailService.sendSimpleEmail("faizanfarooq56g@gmail.com", subject, body);

        return ResponseEntity.ok("Message sent successfully");
    }


    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUserDetails() {
        return ResponseEntity.ok(adminService.getAccountDetailsByRole());
    }

    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordRequest request) {
        System.out.println(request.getCurrentPassword());
        System.out.println(request.getNewPassword());
        adminService.changePassword(request);
        return ResponseEntity.ok("Password changed successfully");
    }


    // Single endpoint to search users by any field, query param is partial input
    @GetMapping("/search/student/email")
    public ResponseEntity<List<StudentDTO>> searchStudentByEmail(@RequestParam String email) {
        List<StudentDTO> studentDTOs = adminService.searchStudentByEmail(email);
        return ResponseEntity.ok(studentDTOs);
    }

    @GetMapping("/search/teacher/email")
    public ResponseEntity<List<Teacher>> searchTeacherByEmail(@RequestParam String email) {
        List<Teacher> teachers = adminService.searchTeacherByEmail(email);
        return ResponseEntity.ok(teachers);
    }







}



