package com.autoevaluator.adapter.handler.rest;

import com.autoevaluator.application.QuestionPaperPdfGenerator;
import com.autoevaluator.application.TeacherService;

import com.autoevaluator.domain.dto.*;
import com.autoevaluator.domain.entity.AppUser;
import com.autoevaluator.domain.models.UserPrincipal;
import com.autoevaluator.domain.repositories.AppUserRepository;
import com.autoevaluator.domain.repositories.QuestionPaperRepository;
import com.autoevaluator.util.InMemoryMultipartFile;
import com.autoevaluator.util.TaskRateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;



import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;



import java.io.IOException;
import java.util.ArrayList;
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

    private final QuestionPaperRepository questionPaperRepository;

    private final TaskRateLimiter taskRateLimiter;
    public TeacherController(TeacherService teacherService, AppUserRepository appUserRepository, TaskRateLimiter taskRateLimiter, QuestionPaperRepository questionPaperRepository) {
        this.teacherService = teacherService;
        this.appUserRepository = appUserRepository;
        this.taskRateLimiter = taskRateLimiter;
        this.questionPaperRepository = questionPaperRepository;
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




        teacherService.addQuestionToPaper(paperId, questionDTO);

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

    @GetMapping("/studentDashboard/single")
    public ResponseEntity<StudentResponse> getStudentDashboardInfoByUsername(
            @RequestParam String collegeName,
            @RequestParam String departmentName,
            @RequestParam int semester,
            @RequestParam String courseName,
            @RequestParam String studentUsername
    ) {
        StudentResponse response = teacherService.getStudentDashboardInfoByUsername(
                collegeName, departmentName, semester, courseName, studentUsername
        );
        return ResponseEntity.ok(response);
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


        List<StudentResponse> dashboard = teacherService.getStudentDashboardInfo(collegeName,departmentName, semester, courseName);

        return ResponseEntity.ok(dashboard);
    }@PostMapping("/uploadMidtermAnswerSheet")
    public ResponseEntity<?> uploadMidtermAnswerSheet(
            @RequestParam String studentUsername,
            @RequestParam String courseName,
            @RequestParam("files") List<MultipartFile> files) {


        boolean exists = questionPaperRepository
                .findByCourse_CourseNameAndIsMidtermTrue(courseName)
                .isPresent();

        if (!exists) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                            "❌ Midterm question paper not created yet, upload rejected"));
        }


        if (!taskRateLimiter.canExecute(studentUsername, courseName, "UPLOAD_MIDTERM", null)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorResponse(HttpStatus.TOO_MANY_REQUESTS.value(),
                            "🕒 Please wait at least 1 minute before re-uploading midterm for this student."));
        }


        for (MultipartFile file : files) {
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                                "❌ Only image files (JPG, PNG) are allowed. PDFs are not supported in this upload."));
            }
        }

        try {

            List<MultipartFile> inMemoryFiles = new ArrayList<>();
            for (MultipartFile file : files) {
                inMemoryFiles.add(new InMemoryMultipartFile(
                        file.getBytes(),
                        file.getOriginalFilename(),
                        file.getContentType()
                ));
            }

            String teacherUsername = getCurrentUser().getUsername();


            teacherService.uploadMidtermSheetAsync(studentUsername, courseName, inMemoryFiles, teacherUsername);
            taskRateLimiter.recordExecution(studentUsername, courseName, "UPLOAD_MIDTERM", null);

            return ResponseEntity.accepted().body(Map.of(
                    "message", "✅ Midterm image answer sheets received. Processing started — you will be notified once complete."
            ));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to read uploaded files"
            ));
        }
    }

    @PostMapping("/uploadEndtermAnswerSheet")
    public ResponseEntity<?> uploadEndtermAnswerSheet(
            @RequestParam String studentUsername,
            @RequestParam String courseName,
            @RequestParam("files") List<MultipartFile> files) {


        boolean exists = questionPaperRepository
                .findByCourse_CourseNameAndIsEndtermTrue(courseName)
                .isPresent();

        if (!exists) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                            "❌ Endterm question paper not created yet, upload rejected"));
        }

        if (!taskRateLimiter.canExecute(studentUsername, courseName, "UPLOAD_ENDTERM", null)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorResponse(HttpStatus.TOO_MANY_REQUESTS.value(),
                            "🕒 Please wait at least 1 minute before re-uploading endterm for this student."));
        }

        try {
            String teacherUsername = getCurrentUser().getUsername();


            for (MultipartFile file : files) {
                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    return ResponseEntity.badRequest()
                            .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                                    "❌ All uploaded files must be images (JPG/PNG)."));
                }
            }

            teacherService.uploadEndtermSheetAsync(studentUsername, courseName, files, teacherUsername);
            taskRateLimiter.recordExecution(studentUsername, courseName, "UPLOAD_ENDTERM", null);

            return ResponseEntity.accepted().body(Map.of(
                    "message", "✅ Endterm answer sheets received. Processing started — you will be notified once complete."
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to process uploaded files"
            ));
        }
    }

    @PostMapping("/uploadAssignmentAnswerSheet")
    public ResponseEntity<?> uploadAssignmentAnswerSheet(
            @RequestParam String studentUsername,
            @RequestParam String courseName,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam String assignmentNumber) {

        Integer assignmentNum;
        try {
            assignmentNum = Integer.parseInt(assignmentNumber);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(
                    new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                            "❌ Invalid assignment number. Upload rejected"));
        }


        boolean exists = questionPaperRepository
                .findByCourse_CourseNameAndIsAssignmentTrueAndAssignmentNumber(courseName, assignmentNum)
                .isPresent();

        if (!exists) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                            "❌ Assignment not created yet, upload rejected"));
        }


        if (!taskRateLimiter.canExecute(studentUsername, courseName, "UPLOAD_ASSIGNMENT", assignmentNumber)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorResponse(HttpStatus.TOO_MANY_REQUESTS.value(),
                            "🕒 Please wait at least 1 minute before re-uploading assignment for this student."));
        }

        try {

            for (MultipartFile file : files) {
                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    return ResponseEntity.badRequest().body(
                            new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                                    "❌ All uploaded files must be images (JPG/PNG)."));
                }
            }

            String teacherUsername = getCurrentUser().getUsername();
            teacherService.uploadAssignmentSheetAsync(
                    studentUsername, courseName, files, assignmentNumber, teacherUsername);

            taskRateLimiter.recordExecution(studentUsername, courseName, "UPLOAD_ASSIGNMENT", assignmentNumber);

            return ResponseEntity.accepted().body(Map.of(
                    "message", "✅ Assignment answer sheets received. Processing started — you will be notified once complete."
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to read uploaded files"
            ));
        }
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
                    .map(q -> {
                        StringBuilder sb = new StringBuilder();
                        sb.append(q.getQuestionText())
                                .append(" (Marks: ").append(q.getMarks()).append(")");

                        if (q.getInstructions() != null && !q.getInstructions().isBlank()) {
                            sb.append(" - Instructions: ").append(q.getInstructions());
                        }

                        return sb.toString();
                    })
                    .collect(Collectors.toList());



            byte[] pdfBytes = QuestionPaperPdfGenerator.generateQuestionPaper(
                    paper.getCollegeName(),
                    "src/main/resources/static/logo.jpg",
                    paper.getCourseName(),
                    paper.getCourseCode(),
                    paper.getDepartmentName(),
                    paper.getSemester(),
                    paper.getType(),
                    questionsText,
                    examInfo.getExamDate(),
                    examInfo.getExamTime()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=QuestionPaper_" + paper.getType() + id  + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }



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
