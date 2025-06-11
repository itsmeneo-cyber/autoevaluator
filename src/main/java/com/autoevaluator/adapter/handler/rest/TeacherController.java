package com.autoevaluator.adapter.handler.rest;

import com.autoevaluator.application.QuestionPaperPdfGenerator;
import com.autoevaluator.application.TeacherService;

import com.autoevaluator.domain.dto.*;
import com.autoevaluator.domain.entity.AppUser;
import com.autoevaluator.domain.models.UserPrincipal;
import com.autoevaluator.domain.repositories.AppUserRepository;
import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.pdf.canvas.draw.DashedLine;
import com.itextpdf.layout.borders.DashedBorder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Image;
import com.itextpdf.io.image.ImageDataFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;
import java.io.File;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/teacher")
//@CrossOrigin(origins = "http://localhost:3000")
public class TeacherController extends BaseRestController {

    private final TeacherService teacherService;

    private final AppUserRepository appUserRepository;

    public TeacherController(TeacherService teacherService, AppUserRepository appUserRepository) {
        this.teacherService = teacherService;
        this.appUserRepository = appUserRepository;
    }



    @GetMapping("/getCoursesForTeacher")
    public ResponseEntity<?> getCoursesForTeacher() {
        AppUser appUser = getCurrentUser(); // Fetch from authenticated session
        String username = appUser.getUsername();
        List<CourseDTO> courses = teacherService.getCoursesByTeacherUsername(username);
        return ResponseEntity.ok(Map.of("courses", courses));
    }


    @PostMapping(value = "/createPaper", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createQuestionPaper(@RequestBody QuestionPaperRequest questionPaperRequest) {
        Long paperId = teacherService.createQuestionPaper(questionPaperRequest);
        return ResponseEntity.ok().body(Map.of(
                "message", "Question Paper created successfully.",
                "questionPaperId", paperId
        ));
    }

    @GetMapping(value = "/getPaper/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getQuestionPaper(@PathVariable("id") Long id) {
        QuestionPaperResponse questionPaper = teacherService.getQuestionPaperById(id);
        return ResponseEntity.ok().body(Map.of(
                "message", "Question Paper retrieved successfully.",
                "questionPaper", questionPaper
        ));
    }

    @PostMapping(value = "/createPaper/{paperId}/addQuestion", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Add a new question to an existing Question Paper")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Question added successfully."),
            @ApiResponse(responseCode = "404", description = "Question Paper not found."),
            @ApiResponse(responseCode = "403", description = "Unauthorized request.")
    })
    public ResponseEntity<?> addQuestionToPaper(@PathVariable("paperId") Long paperId,
                                                @RequestBody QuestionRequest questionDTO) {

        // ✅ Print incoming request details
        System.out.println("Received request to add question to paper ID: " + paperId);
        System.out.println("QuestionRequest Payload:");
        System.out.println("  Question Number: " + questionDTO.getQuestionNumber());
        System.out.println("  Text: " + questionDTO.getText());
        System.out.println("  Marks: " + questionDTO.getMarks());
        System.out.println("  Instructions: " + questionDTO.getInstructions());
        System.out.println("  Correct Answer: " + questionDTO.getCorrectAnswer());

        // Call the service method to add the question to the paper
        teacherService.addQuestionToPaper(paperId, questionDTO);

        // Return a simple success message
        return ResponseEntity.ok().body(Map.of(
                "message", "Question added successfully."
        ));
    }


    @PutMapping(value = "/{paperId}/updateQuestion/{questionId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a specific Question in a Question Paper")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Question updated successfully."),
            @ApiResponse(responseCode = "404", description = "Question not found."),
            @ApiResponse(responseCode = "403", description = "Unauthorized request.")
    })

    public ResponseEntity<?> updateQuestionInPaper(
            @PathVariable("paperId") Long paperId,
            @PathVariable("questionId") Long questionId,
            @RequestBody QuestionRequest questionRequest) {

        teacherService.updateQuestionInPaper(paperId, questionId, questionRequest);
        return ResponseEntity.ok(Map.of("message", "Question updated successfully."));
    }

    @DeleteMapping(value = "/deletePaper/{paperId}")
    @Operation(summary = "Delete an existing Question Paper by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Question paper deleted successfully."),
            @ApiResponse(responseCode = "404", description = "Question Paper not found."),
            @ApiResponse(responseCode = "403", description = "Unauthorized request.")
    })
    public ResponseEntity<?> deleteQuestionPaper(@PathVariable("paperId") Long paperId) {
        teacherService.deleteQuestionPaper(paperId);
        return ResponseEntity.ok().body(Map.of(
                "message", "Question paper deleted successfully."
        ));
    }

    @DeleteMapping(value = "/{paperId}/deleteQuestion/{questionId}")
    @Operation(summary = "Delete a specific Question from a Question Paper")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Question deleted successfully."),
            @ApiResponse(responseCode = "404", description = "Question or Question Paper not found."),
            @ApiResponse(responseCode = "403", description = "Unauthorized request.")
    })
    public ResponseEntity<?> deleteQuestionFromPaper(@PathVariable("paperId") Long paperId,
                                                     @PathVariable("questionId") Long questionId) {
        teacherService.deleteQuestionFromPaper(paperId, questionId);
        return ResponseEntity.ok().body(Map.of(
                "message", "Question deleted successfully."
        ));
    }


    @PostMapping("/{paperId}/shareWith/{teacherUsername}")
    @Operation(summary = "Share a Question Paper with another Teacher")
    public ResponseEntity<?> sharePaperWithTeacher(@PathVariable Long paperId, @PathVariable String teacherUsername) {
        teacherService.sharePaperWithTeacher(paperId, teacherUsername);
        return ResponseEntity.ok(Map.of("message", "Paper shared successfully."));
    }


    @DeleteMapping("/{paperId}/unshareWith/{teacherUsername}")
    @Operation(summary = "Unshare a Question Paper from a Teacher")
    public ResponseEntity<?> unsharePaperWithTeacher(@PathVariable Long paperId, @PathVariable String teacherUsername) {
        teacherService.unsharePaperWithTeacher(paperId, teacherUsername);
        return ResponseEntity.ok(Map.of("message", "Paper unshared successfully."));
    }

    @GetMapping(value = "/getAllQuestionPapersByTeacher", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<QuestionPaperResponse>> getAllQuestionPapersByTeacher(
            @RequestParam(required = false) String courseName,
            @RequestParam(required = false) Integer semester) {
        List<QuestionPaperResponse> questionPapers = teacherService.getAllQuestionPapersByTeacher(courseName, semester);
        return ResponseEntity.ok(questionPapers);
    }



    @GetMapping("/getStudentPanel")
    @Operation(summary = "Get Student Dashboard Information",
            description = "Fetches student dashboard details based on the department, year, and course.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Student dashboard fetched successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "404", description = "No students found for the provided department, year, and course"),
    })
    public ResponseEntity<List<StudentResponse>> getStudentDashboardInfo(
            @RequestParam String collegeName,
            @RequestParam String departmentName,
            @RequestParam int semester,
            @RequestParam String courseName) {

        // Get student dashboard data from service
        List<StudentResponse> dashboard = teacherService.getStudentDashboardInfo(collegeName,departmentName, semester, courseName);

        return ResponseEntity.ok(dashboard);
    }

    @PostMapping("/uploadMidtermAnswerSheet")
    public ResponseEntity<?> uploadMidtermAnswerSheet(
            @RequestParam String studentUsername,
            @RequestParam String courseName,
            @RequestParam("file") MultipartFile file) {

        String uploadedUrl = teacherService.uploadMidtermSheet(studentUsername, courseName, file);

        return ResponseEntity.ok(Map.of(
                "message", "Midterm answer sheet uploaded successfully",
                "url", uploadedUrl
        ));
    }

    @PostMapping("/uploadEndtermAnswerSheet")
    public ResponseEntity<?> uploadEndtermAnswerSheet(
            @RequestParam String studentUsername,
            @RequestParam String courseName,
            @RequestParam("file") MultipartFile file) {

        String uploadedUrl = teacherService.uploadEndtermSheet(studentUsername, courseName, file);

        return ResponseEntity.ok(Map.of(
                "message", "Endterm answer sheet uploaded successfully",
                "url", uploadedUrl
        ));
    }

    @PostMapping("/uploadAssignmentAnswerSheet")
    public ResponseEntity<?> uploadAssignmentAnswerSheet(
            @RequestParam String studentUsername,
            @RequestParam String courseName,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String assignmentNumber) {

        String uploadedUrl = teacherService.uploadAssignmentSheet(studentUsername, courseName, file, assignmentNumber);

        return ResponseEntity.ok(Map.of(
                "message", "Assignment answer sheet uploaded successfully",
                "url", uploadedUrl
        ));
    }

    @PostMapping("/deleteMidtermAnswerSheet")
    public ResponseEntity<?> deleteMidtermAnswerSheet(
            @RequestParam String studentUsername,
            @RequestParam String courseName) {

        teacherService.deleteMidtermSheet(studentUsername, courseName);

        return ResponseEntity.ok(Map.of(
                "message", "Midterm answer sheet deleted successfully"
        ));
    }

    @PostMapping("/deleteEndtermAnswerSheet")
    public ResponseEntity<?> deleteEndtermAnswerSheet(
            @RequestParam String studentUsername,
            @RequestParam String courseName) {

        teacherService.deleteEndtermSheet(studentUsername, courseName);

        return ResponseEntity.ok(Map.of(
                "message", "Endterm answer sheet deleted successfully"
        ));
    }

    @PostMapping("/deleteAssignmentAnswerSheet")
    public ResponseEntity<?> deleteAssignmentAnswerSheet(
            @RequestParam String studentUsername,
            @RequestParam String courseName,
            @RequestParam String assignmentNumber) {

        teacherService.deleteAssignmentSheet(studentUsername, courseName, assignmentNumber);

        return ResponseEntity.ok(Map.of(
                "message", "Assignment answer sheet deleted successfully"
        ));
    }
    @PostMapping(value = "/downloadQuestionPaper/{id}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadQuestionPaper(
            @PathVariable Long id,
            @RequestBody ExamDateTimeRequest examInfo) {
        try {
            QuestionPaperResponse paper = teacherService.getQuestionPaperById(id);

            List<String> questionsText = paper.getQuestions().stream()
                    .map(q -> q.getQuestionText() + " (Marks: " + q.getMarks() + ")")
                    .collect(Collectors.toList());

            // Pass exam date/time if your PDF generator uses it
            byte[] pdfBytes = QuestionPaperPdfGenerator.generateQuestionPaper(
                    paper.getCollegeName(),
                    "src/main/resources/static/logo.jpg",
                    paper.getCourseName(),
                    paper.getCourseCode(),
                    paper.getDepartmentName(),
                    paper.getSemester(),
                    paper.getType(),
                    questionsText,
                    examInfo.getExamDate(),     // new param
                    examInfo.getExamTime()      // new param
            );

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=QuestionPaper_" + id + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }


//
//    /**
//     * Check if a teacher is assigned to a specific department and course in a given semester.
//     *
//     * @param username       The username of the teacher (typically their login/email ID).
//     * @param departmentName The name of the department to check assignment against.
//     * @param courseName     The name of the course the teacher should be assigned to.
//     * @param semester       The semester number of the course.
//     * @return ResponseEntity containing true if the teacher is assigned, false otherwise.
//     */
//    @Operation(
//            summary = "Check if a teacher is assigned to a department and course in a given semester",
//            description = "Returns true if the teacher with the given username is assigned to the specified department and course for the given semester."
//    )
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "Successfully verified teacher assignment"),
//            @ApiResponse(responseCode = "404", description = "Teacher not found"),
//            @ApiResponse(responseCode = "400", description = "Invalid input parameters")
//    })
//    @GetMapping("/teacher/assigned")
//    public ResponseEntity<Boolean> isTeacherAssigned(
//            @RequestParam String username,
//            @RequestParam String departmentName,
//            @RequestParam String courseName,
//            @RequestParam int semester
//    ) {
//        boolean assigned = teacherService.isTeacherAssigned(username, departmentName, courseName, semester);
//        return ResponseEntity.ok(assigned);
//    }

    @GetMapping("/shareList")
    @Operation(summary = "Get all teachers except the one with given username")
    public ResponseEntity<List<TeacherDTO>> getAllTeachersExceptCurrent() {
        List<TeacherDTO> teachers = teacherService.getAllTeachersExceptCurrent();
        return ResponseEntity.ok(teachers);
    }

    @PostMapping("/assignAsMidterm/{paperId}")
    @Operation(summary = "Assign a paper as Midterm")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assigned as midterm successfully."),
            @ApiResponse(responseCode = "404", description = "Question Paper not found.")
    })
    public ResponseEntity<?> assignAsMidterm(@PathVariable Long paperId) {
        teacherService.assignPaperAsMidterm(paperId);
        return ResponseEntity.ok(Map.of("message", "Paper assigned as Midterm successfully"));
    }

    @PostMapping("/assignAsEndterm/{paperId}")
    @Operation(summary = "Assign a paper as Endterm")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assigned as endterm successfully."),
            @ApiResponse(responseCode = "404", description = "Question Paper not found.")
    })
    public ResponseEntity<?> assignAsEndterm(@PathVariable Long paperId) {
        teacherService.assignPaperAsEndterm(paperId);
        return ResponseEntity.ok(Map.of("message", "Paper assigned as Endterm successfully"));
    }


    private AppUser getCurrentUser() {
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email = userPrincipal.getUsername();
        return appUserRepository.findByUsername(email);
    }



}
