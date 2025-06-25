package com.autoevaluator.adapter.handler.rest;

import com.autoevaluator.application.EvaluationService;
import com.autoevaluator.domain.dto.AnswerScoreDto;
import com.autoevaluator.domain.dto.BulkEvaluateRequest;
import com.autoevaluator.domain.dto.EvaluationResponseDto;
import com.autoevaluator.domain.entity.AppUser;
import com.autoevaluator.domain.models.UserPrincipal;
import com.autoevaluator.domain.repositories.AppUserRepository;
import com.autoevaluator.util.TaskRateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/evaluate")
@RequiredArgsConstructor
//@CrossOrigin(origins = "http://localhost:3000")
public class EvaluationController extends BaseRestController {

    private final EvaluationService evaluationService;
    private final AppUserRepository appUserRepository;
    private static final Logger log = LoggerFactory.getLogger(EvaluationController.class);
    @Autowired
    private TaskRateLimiter taskRateLimiter;

    // ✅ Midterm Evaluation
    @PostMapping("/evaluateMidterm")
    @Operation(summary = "Evaluate Midterm",
            description = "Evaluates the midterm for a student in a specific course and returns detailed results.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Midterm evaluation accepted and started."),
            @ApiResponse(responseCode = "429", description = "Too many requests.")
    })
    public ResponseEntity<?> evaluateMidterm(@RequestParam String studentUsername,
                                             @RequestParam String courseName) throws Exception {

        if (!taskRateLimiter.canExecute(studentUsername, courseName, "EVALUATE_MIDTERM", null)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorResponse(HttpStatus.TOO_MANY_REQUESTS.value(),
                            "🕒 Please wait at least 1 minute before re-evaluating midterm."));
        }
        String teacherUsername = getCurrentUser().getUsername();

        evaluationService.evaluateMidtermAsync(studentUsername, courseName, teacherUsername); // <- will make this async in service
        taskRateLimiter.recordExecution(studentUsername, courseName, "EVALUATE_MIDTERM", null);

        return ResponseEntity.accepted().body(Map.of(
                "message", "✅ Midterm evaluation started. You will be notified once complete."
        ));
    }@PostMapping("/evaluateEndterm")
    @Operation(summary = "Evaluate Endterm",
            description = "Evaluates the endterm for a student in a specific course and returns detailed results.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Endterm evaluation accepted and started."),
            @ApiResponse(responseCode = "429", description = "Too many requests.")
    })
    public ResponseEntity<?> evaluateEndterm(@RequestParam String studentUsername,
                                             @RequestParam String courseName) {

        System.out.println("🔍 Current Thread: " + Thread.currentThread().getName());
        if (!taskRateLimiter.canExecute(studentUsername, courseName, "EVALUATE_ENDTERM", null)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorResponse(HttpStatus.TOO_MANY_REQUESTS.value(),
                            "🕒 Please wait at least 1 minute before re-evaluating endterm."));
        }

        String teacherUsername = getCurrentUser().getUsername();
        evaluationService.evaluateEndtermAsync(studentUsername, courseName, teacherUsername); // <- async method in service
        taskRateLimiter.recordExecution(studentUsername, courseName, "EVALUATE_ENDTERM", null);

        return ResponseEntity.accepted().body(Map.of(
                "message", "✅ Endterm evaluation started. You will be notified once complete."
        ));
    }@PostMapping("/evaluateAssignment")
    @Operation(summary = "Evaluate Assignment",
            description = "Evaluates a specific assignment for a student in a course.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Assignment evaluation accepted and started."),
            @ApiResponse(responseCode = "429", description = "Too many requests.")
    })
    public ResponseEntity<?> evaluateAssignment(@RequestParam String studentUsername,
                                                @RequestParam String courseName,
                                                @RequestParam int assignmentNo) {

        String assignmentKey = String.valueOf(assignmentNo);
        if (!taskRateLimiter.canExecute(studentUsername, courseName, "EVALUATE_ASSIGNMENT", assignmentKey)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorResponse(HttpStatus.TOO_MANY_REQUESTS.value(),
                            "🕒 Please wait at least 1 minute before re-evaluating assignment."));
        }

        String teacherUsername = getCurrentUser().getUsername();
        evaluationService.evaluateAssignmentAsync(studentUsername, courseName, assignmentNo,teacherUsername); // <- async
        taskRateLimiter.recordExecution(studentUsername, courseName, "EVALUATE_ASSIGNMENT", assignmentKey);

        return ResponseEntity.accepted().body(Map.of(
                "message", "✅ Assignment evaluation started. You will be notified once complete."
        ));
    }
    @PostMapping("/bulkEvaluateAnswerSheets")
    @Operation(summary = "Bulk Evaluate Answer Sheets",
            description = "Initiates evaluation for all uploaded answer sheets of a given type in a course.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Bulk evaluation accepted and started."),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters.")
    })
    public ResponseEntity<?> bulkEvaluateAnswerSheets(@RequestBody BulkEvaluateRequest request) throws Exception {
        log.info("[BULK_EVALUATE] Received bulk evaluate request: {}", request);

        String courseName = request.getCourseName();
        String type = request.getEvaluationType(); // MIDTERM, ENDTERM, ASSIGNMENT
        Integer assignmentNumber = request.getAssignmentNumber();
        String teacherUsername = getCurrentUser().getUsername();

        if (courseName == null || type == null) {
            log.warn("[BULK_EVALUATE] Missing courseName or type in request: {}", request);
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(400, "❌ courseName and type are required."));
        }

        log.info("[BULK_EVALUATE] Starting bulk evaluation for course: {}, type: {}, teacher: {}", courseName, type, teacherUsername);

        switch (type.toUpperCase()) {
            case "MIDTERM":
                log.info("[BULK_EVALUATE] Triggering bulkEvaluateMidtermAsync");
                evaluationService.bulkEvaluateMidtermAsync(courseName, teacherUsername);
                break;

            case "ENDTERM":
                log.info("[BULK_EVALUATE] Triggering bulkEvaluateEndtermAsync");
                evaluationService.bulkEvaluateEndtermAsync(courseName, teacherUsername);
                break;

            case "ASSIGNMENT":
                if (assignmentNumber == null) {
                    log.warn("[BULK_EVALUATE] assignmentNumber is required for ASSIGNMENT evaluation but was null");
                    return ResponseEntity.badRequest()
                            .body(new ErrorResponse(400, "❌ assignmentNumber is required for ASSIGNMENT evaluation."));
                }
                log.info("[BULK_EVALUATE] Triggering bulkEvaluateAssignmentAsync for assignmentNumber: {}", assignmentNumber);
                evaluationService.bulkEvaluateAssignmentAsync(courseName, assignmentNumber, teacherUsername);
                break;

            default:
                log.warn("[BULK_EVALUATE] Invalid sheet type received: {}", type);
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse(400, "❌ Invalid sheet type. Allowed: MIDTERM, ENDTERM, ASSIGNMENT."));
        }

        log.info("[BULK_EVALUATE] Bulk evaluation accepted and started for course: {}", courseName);

        return ResponseEntity.accepted().body(Map.of(
                "message", "✅ Bulk evaluation started. You will be notified as evaluations complete."
        ));
    }



    @GetMapping("/viewMidterm")
    @Operation(summary = "View Midterm Answer Scores",
            description = "Returns all midterm answer scores for a student in a specific course.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Midterm scores fetched successfully."),
            @ApiResponse(responseCode = "404", description = "Student or course not found."),
            @ApiResponse(responseCode = "400", description = "Invalid input data.")
    })
    public ResponseEntity<List<AnswerScoreDto>> viewMidterm(@RequestParam String studentUsername,
                                                            @RequestParam String courseName) {
        List<AnswerScoreDto> scores = evaluationService.viewMidtermScores(studentUsername, courseName);
        return ResponseEntity.ok(scores);
    }


    @GetMapping("/viewEndterm")
    @Operation(summary = "View Endterm Answer Scores",
            description = "Returns all endterm answer scores for a student in a specific course.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Endterm scores fetched successfully."),
            @ApiResponse(responseCode = "404", description = "Student or course not found."),
            @ApiResponse(responseCode = "400", description = "Invalid input data.")
    })
    public ResponseEntity<List<AnswerScoreDto>> viewEndterm(@RequestParam String studentUsername,
                                                            @RequestParam String courseName) {
        List<AnswerScoreDto> scores = evaluationService.viewEndtermScores(studentUsername, courseName);
        return ResponseEntity.ok(scores);
    }

    @GetMapping("/viewAssignment")
    @Operation(summary = "View Assignment Answer Scores",
            description = "Returns all answer scores for a specific assignment submitted by a student.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assignment scores fetched successfully."),
            @ApiResponse(responseCode = "404", description = "Student, course or assignment not found."),
            @ApiResponse(responseCode = "400", description = "Invalid input data.")
    })
    public ResponseEntity<List<AnswerScoreDto>> viewAssignment(@RequestParam String studentUsername,
                                                               @RequestParam String courseName,
                                                               @RequestParam int assignmentNo) {
        List<AnswerScoreDto> scores = evaluationService.viewAssignmentScores(studentUsername, courseName, assignmentNo);
        return ResponseEntity.ok(scores);
    }
    @GetMapping("/viewMidtermRaw")
    public ResponseEntity<List<AnswerScoreDto>> viewMidtermRaw(@RequestParam String studentUsername,
                                                               @RequestParam String courseName) {
        List<AnswerScoreDto> answers = evaluationService.viewMidtermRawAnswers(studentUsername, courseName);
        return ResponseEntity.ok(answers);
    }

    @GetMapping("/viewEndtermRaw")
    public ResponseEntity<List<AnswerScoreDto>> viewEndtermRaw(@RequestParam String studentUsername,
                                                               @RequestParam String courseName) {
        List<AnswerScoreDto> answers = evaluationService.viewEndtermRawAnswers(studentUsername, courseName);
        return ResponseEntity.ok(answers);
    }

    @GetMapping("/viewAssignmentRaw")
    public ResponseEntity<List<AnswerScoreDto>> viewAssignmentRaw(@RequestParam String studentUsername,
                                                                  @RequestParam String courseName,
                                                                  @RequestParam int assignmentNo) {
        List<AnswerScoreDto> answers = evaluationService.viewAssignmentRawAnswers(studentUsername, courseName, assignmentNo);
        return ResponseEntity.ok(answers);
    }



    private AppUser getCurrentUser() {
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email = userPrincipal.getUsername();
        return appUserRepository.findByUsername(email);
    }

}
