package com.autoevaluator.adapter.handler.rest;

import com.autoevaluator.application.EvaluationService;
import com.autoevaluator.domain.dto.AnswerScoreDto;
import com.autoevaluator.domain.dto.EvaluationResponseDto;
import com.autoevaluator.domain.entity.AppUser;
import com.autoevaluator.domain.models.UserPrincipal;
import com.autoevaluator.domain.repositories.AppUserRepository;
import com.autoevaluator.util.TaskRateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
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
                                             @RequestParam String courseName) {

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
