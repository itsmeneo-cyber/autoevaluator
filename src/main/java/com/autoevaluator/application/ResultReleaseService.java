package com.autoevaluator.application;

import com.autoevaluator.domain.entity.*;
import com.autoevaluator.domain.entity.AnswerSheetType;
import com.autoevaluator.domain.repositories.CourseRepository;
import com.autoevaluator.domain.repositories.EnrolmentRepository;
import com.autoevaluator.domain.repositories.ReleaseStatusRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResultReleaseService {

    private final CourseRepository courseRepository;
    private final ReleaseStatusRepository releaseStatusRepository;
    private final EnrolmentRepository enrolmentRepository;

    public Map<String, Boolean> getReleaseStatus(String courseCode) {
        Course course = courseRepository.findByCourseCode(courseCode)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Map<String, Boolean> statusMap = new HashMap<>();
        releaseStatusRepository.findByCourse(course).forEach(status -> {
            String key = status.getAnswerSheetType().name();
            if (status.getAnswerSheetType() == AnswerSheetType.ASSIGNMENT && status.getAssignmentNumber() != null) {
                key += "_" + status.getAssignmentNumber();
            }
            statusMap.put(key, status.isReleased());
        });

        return statusMap;
    }

    public void setReleaseStatus(String courseCode, AnswerSheetType type, boolean status, Integer assignmentNo) {
        Course course = courseRepository.findByCourseCode(courseCode)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        ReleaseStatus releaseStatus = (type == AnswerSheetType.ASSIGNMENT)
                ? releaseStatusRepository.findByCourseAndAnswerSheetTypeAndAssignmentNumber(course, type, assignmentNo).orElse(new ReleaseStatus())
                : releaseStatusRepository.findByCourseAndAnswerSheetType(course, type).orElse(new ReleaseStatus());

        releaseStatus.setCourse(course);
        releaseStatus.setAnswerSheetType(type);
        releaseStatus.setReleased(status);
        releaseStatus.setAssignmentNumber(type == AnswerSheetType.ASSIGNMENT ? assignmentNo : null);

        releaseStatusRepository.save(releaseStatus);
    }
    public void downloadResult(String courseCode, AnswerSheetType type, String assignmentNumber, HttpServletResponse response) throws IOException {
        Course course = courseRepository.findByCourseCode(courseCode)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        List<Enrolment> enrolments = enrolmentRepository.findByCourse(course);

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=result.csv");

        PrintWriter writer = response.getWriter();

        if (type == AnswerSheetType.ALL) {
            // 1. Collect all unique assignment numbers
            Set<String> allAssignmentNumbers = new TreeSet<>();
            for (Enrolment e : enrolments) {
                for (AssignmentSubmission a : e.getAssignments()) {
                    allAssignmentNumbers.add(a.getAssignmentNumber());
                }
            }

            // 2. Write header
            writer.print("Roll No,Name,MIDTERM,ENDTERM");
            for (String num : allAssignmentNumbers) {
                writer.printf(",ASSIGNMENT_%s", num);
            }
            writer.println();

            // 3. Write data rows
            for (Enrolment e : enrolments) {
                String roll = e.getStudent().getRollNo();
                String name = e.getStudent().getName();
                double midterm = e.getMidtermMarks() != null ? e.getMidtermMarks() : 0.00;
                double endterm = e.getEndtermMarks() != null ? e.getEndtermMarks() : 0.00;

                writer.printf("%s,%s,%.2f,%.2f", roll, name, midterm, endterm);

                // Create map of assignmentNumber -> marks
                Map<String, Double> assignmentMap = e.getAssignments().stream()
                        .collect(Collectors.toMap(
                                AssignmentSubmission::getAssignmentNumber,
                                a -> a.getMarks() != null ? a.getMarks() : 0.00
                        ));

                for (String num : allAssignmentNumbers) {
                    double marks = assignmentMap.getOrDefault(num, 0.00);
                    writer.printf(",%.2f", marks);
                }

                writer.println();
            }

        } else {
            // Non-ALL behavior (as previously written)
            writer.println("Roll No,Name,Type,Assignment Number,Marks");

            for (Enrolment e : enrolments) {
                String roll = e.getStudent().getRollNo();
                String name = e.getStudent().getName();

                switch (type) {
                    case MIDTERM -> {
                        double marks = e.getMidtermMarks() != null ? e.getMidtermMarks() : 0.00;
                        writer.printf("%s,%s,MIDTERM,,%.2f%n", roll, name, marks);
                    }
                    case ENDTERM -> {
                        double marks = e.getEndtermMarks() != null ? e.getEndtermMarks() : 0.00;
                        writer.printf("%s,%s,ENDTERM,,%.2f%n", roll, name, marks);
                    }
                    case ASSIGNMENT -> {
                        if (assignmentNumber == null)
                            throw new IllegalArgumentException("Assignment number is required");

                        e.getAssignments().stream()
                                .filter(a -> assignmentNumber.equals(a.getAssignmentNumber()))
                                .findFirst()
                                .ifPresent(a -> {
                                    double marks = a.getMarks() != null ? a.getMarks() : 0.00;
                                    writer.printf("%s,%s,ASSIGNMENT,%s,%.2f%n", roll, name, assignmentNumber, marks);
                                });
                    }
                }
            }
        }

        writer.flush();
    }

}
