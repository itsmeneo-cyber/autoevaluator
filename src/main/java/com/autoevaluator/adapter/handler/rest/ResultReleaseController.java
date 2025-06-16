package com.autoevaluator.adapter.handler.rest;

import com.autoevaluator.application.ResultReleaseService;
import com.autoevaluator.domain.entity.AnswerSheetType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
@RestController
@RequestMapping("/api/evaluate")
@RequiredArgsConstructor
public class ResultReleaseController {

    private final ResultReleaseService resultReleaseService;

    @GetMapping("/getRelease")
    public ResponseEntity<Map<String, Boolean>> getReleaseStatus(@RequestParam String courseCode) {
        return ResponseEntity.ok(resultReleaseService.getReleaseStatus(courseCode));
    }

    @PostMapping("/setRelease")
    public ResponseEntity<Void> setReleaseStatus(@RequestBody Map<String, Object> payload) {
        String courseCode = (String) payload.get("courseCode");
        AnswerSheetType type = AnswerSheetType.valueOf((String) payload.get("type"));
        boolean status = (Boolean) payload.get("status");

        Integer assignmentNo = null;
        if (type == AnswerSheetType.ASSIGNMENT && payload.containsKey("assignmentNo")) {
            Object value = payload.get("assignmentNo");
            if (value != null) {
                assignmentNo = Integer.valueOf(value.toString());
            }
        }


        resultReleaseService.setReleaseStatus(courseCode, type, status, assignmentNo);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/download-result")
    public void downloadResult(
            @RequestParam String courseCode,
            @RequestParam AnswerSheetType type,
            @RequestParam(required = false) String assignmentNumber,
            HttpServletResponse response
    ) throws IOException {
        resultReleaseService.downloadResult(courseCode, type, assignmentNumber, response);
    }

}

