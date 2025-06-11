package com.autoevaluator.adapter.handler.rest;

import com.autoevaluator.application.EvaluationService;
import com.autoevaluator.domain.dto.AnswerScoreDto;
import com.autoevaluator.domain.dto.EvaluationResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evaluate")
@RequiredArgsConstructor
//@CrossOrigin(origins = "http://localhost:3000")
public class EvaluationController extends BaseRestController {

    private final EvaluationService evaluationService;

    // ✅ Midterm Evaluation
    @PostMapping("/evaluateMidterm")
    @Operation(summary = "Evaluate Midterm",
            description = "Evaluates the midterm for a student in a specific course and returns detailed results.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Midterm evaluation completed."),
            @ApiResponse(responseCode = "404", description = "Student or course not found."),
            @ApiResponse(responseCode = "400", description = "Invalid input data.")
    })
    public ResponseEntity<List<EvaluationResponseDto>> evaluateMidterm(@RequestParam String studentUsername,
                                                                       @RequestParam String courseName) {
        List<EvaluationResponseDto> result = evaluationService.evaluateMidterm(studentUsername, courseName);
        return ResponseEntity.ok(result);
    }

    // ✅ Endterm Evaluation
    @PostMapping("/evaluateEndterm")
    @Operation(summary = "Evaluate Endterm",
            description = "Evaluates the endterm for a student in a specific course and returns detailed results.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Endterm evaluation completed."),
            @ApiResponse(responseCode = "404", description = "Student or course not found."),
            @ApiResponse(responseCode = "400", description = "Invalid input data.")
    })
    public ResponseEntity<List<EvaluationResponseDto>> evaluateEndterm(@RequestParam String studentUsername,
                                                                       @RequestParam String courseName) {
        List<EvaluationResponseDto> result = evaluationService.evaluateEndterm(studentUsername, courseName);
        return ResponseEntity.ok(result);
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


    // ✅ Assignment Evaluation
    @PostMapping("/evaluateAssignment")
    @Operation(summary = "Evaluate Assignment",
            description = "Evaluates a specific assignment for a student in a course.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assignment evaluation completed."),
            @ApiResponse(responseCode = "404", description = "Student, course or assignment not found."),
            @ApiResponse(responseCode = "400", description = "Invalid input data.")
    })
    public ResponseEntity<Double> evaluateAssignment(@RequestParam String studentUsername,
                                                     @RequestParam String courseName,
                                                     @RequestParam int assignmentNo) {
        Double score = evaluationService.evaluateAssignment(studentUsername, courseName, assignmentNo);
        return ResponseEntity.ok(score);
    }
}
