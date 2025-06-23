package com.autoevaluator.application;

import com.autoevaluator.adapter.handler.rest.ErrorResponse;
import com.autoevaluator.domain.dto.AnswerScoreDto;
import com.autoevaluator.domain.dto.CompareAnswersRequest;
import com.autoevaluator.domain.dto.CompareAnswersResponse;
import com.autoevaluator.domain.dto.EvaluationResponseDto;
import com.autoevaluator.domain.entity.*;
import com.autoevaluator.domain.exception.BadRequestException;
import com.autoevaluator.domain.repositories.CourseRepository;
import com.autoevaluator.domain.repositories.EnrolmentRepository;
import com.autoevaluator.domain.repositories.QuestionPaperRepository;
import com.autoevaluator.domain.repositories.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EnrolmentRepository enrolmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final QuestionPaperRepository questionPaperRepository;


    @Value("${spring.scoring.api-url}")
    private String scoringApiUrl;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Async("externalTaskExecutor")
    @Transactional
    public void evaluateMidtermAsync(String studentUsername, String courseName, String teacherUsername) {
        Student student = studentRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        try {

            List<EvaluationResponseDto> result = evaluateMidterm(student, courseName);
            double total = result.stream().mapToDouble(EvaluationResponseDto::getMarksObtained).sum();



            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "MIDTERM_EVALUATION_SUCCESS");
            payload.put("message", "✅ Midterm evaluated. You can now view the answer sheet.");
            payload.put("studentUsername", student.getRollNo());
            payload.put("courseName", courseName);
            payload.put("totalMarks", total);
            payload.put("collegeName", student.getCollege().getName());
            payload.put("departmentName", student.getDepartment().getName());
            payload.put("semester", student.getSemester());



            notifyStructuredClient(teacherUsername, payload);

        } catch (Exception e) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "MIDTERM_EVALUATION_FAILURE");
            payload.put("message", " Midterm evaluation failed: " + e.getMessage());
            payload.put("studentUsername", student.getRollNo());
            payload.put("courseName", courseName);
            notifyStructuredClient(teacherUsername, payload);
        }
    }
    @Async("externalTaskExecutor")
    @Transactional
    public void evaluateEndtermAsync(String studentUsername, String courseName, String teacherUsername) {
        Student student = studentRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        try {

            List<EvaluationResponseDto> result = evaluateEndterm(student, courseName);
            double total = result.stream().mapToDouble(EvaluationResponseDto::getMarksObtained).sum();



            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "ENDTERM_EVALUATION_SUCCESS");
            payload.put("message", "✅ Endterm evaluated. You can now view the answer sheet.");
            payload.put("studentUsername", student.getRollNo());
            payload.put("courseName", courseName);
            payload.put("totalMarks", total);
            payload.put("collegeName", student.getCollege().getName());
            payload.put("departmentName", student.getDepartment().getName());
            payload.put("semester", student.getSemester());

            notifyStructuredClient(teacherUsername, payload);

        } catch (Exception e) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "ENDTERM_EVALUATION_FAILURE");
            payload.put("message", " Endterm evaluation failed: " + e.getMessage());
            payload.put("studentUsername", student.getRollNo());
            payload.put("courseName", courseName);
            notifyStructuredClient(teacherUsername, payload);
        }
    }
    @Async("externalTaskExecutor")
    @Transactional
    public void evaluateAssignmentAsync(String studentUsername, String courseName, int assignmentNo, String teacherUsername) {
        Student student = studentRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        try {


            Double score = evaluateAssignment(student, courseName, assignmentNo);


            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "ASSIGNMENT_EVALUATION_SUCCESS");
            payload.put("message", "✅ Assignment evaluated. You can now view the answer sheet.");
            payload.put("studentUsername", student.getRollNo());
            payload.put("courseName", courseName);
            payload.put("assignmentNumber", assignmentNo);
            payload.put("marks", score);
            payload.put("collegeName", student.getCollege().getName());
            payload.put("departmentName", student.getDepartment().getName());
            payload.put("semester", student.getSemester());

            notifyStructuredClient(teacherUsername, payload);

        } catch (Exception e) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "ASSIGNMENT_EVALUATION_FAILURE");
            payload.put("message", " Assignment evaluation failed: " + e.getMessage());
            payload.put("studentUsername", student.getRollNo());
            payload.put("courseName", courseName);
            payload.put("assignmentNumber", assignmentNo);
            notifyStructuredClient(teacherUsername, payload);
        }
    }

    private void notifyStructuredClient(String teacherUsername, Map<String, Object> payload) {

        messagingTemplate.convertAndSend("/topic/evaluations/" + teacherUsername, payload);
    }

    public List<EvaluationResponseDto> evaluateMidterm(Student student, String courseName) throws Exception {


        Course course = courseRepository.findByCourseName(courseName)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Enrolment enrolment = student.getEnrolments().stream()
                .filter(e -> e.getCourse().getId().equals(course.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Enrolment not found for this student and course"));

        String answerText = enrolment.getMidtermAnswerSheetText();

        if (answerText.isEmpty())
            throw new RuntimeException(String.format("❌ No answer sheet found for student: %s", student.getRollNo()));

        QuestionPaper paper = questionPaperRepository.findByCourse_CourseNameAndIsMidtermTrue(courseName)
                .orElseThrow(() -> new RuntimeException("Midterm paper not found"));

        List<EvaluationResponseDto> dtos = evaluate(answerText, paper, enrolment, AnswerSheetType.MIDTERM,null);

        double total = dtos.stream().mapToDouble(EvaluationResponseDto::getMarksObtained).sum();
        enrolment.setMidtermMarks((double) total);
        enrolmentRepository.save(enrolment);

        return dtos;
    }

    public List<EvaluationResponseDto> evaluateEndterm(Student student, String courseName) {

        Course course = courseRepository.findByCourseName(courseName)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Enrolment enrolment = student.getEnrolments().stream()
                .filter(e -> e.getCourse().getId().equals(course.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Enrolment not found for this student and course"));

        String answerText = enrolment.getEndtermAnswerSheetText();

        QuestionPaper paper = questionPaperRepository.findByCourse_CourseNameAndIsEndtermTrue(courseName)
                .orElseThrow(() -> new BadRequestException("Endterm paper not found , can't evaluate"));

        List<EvaluationResponseDto> dtos = evaluate(answerText, paper, enrolment, AnswerSheetType.ENDTERM,null);

        double total = dtos.stream().mapToDouble(EvaluationResponseDto::getMarksObtained).sum();
        enrolment.setEndtermMarks((double) total);
        enrolmentRepository.save(enrolment);

        return dtos;
    }
    public Double evaluateAssignment(Student student, String courseName, int assignmentNo) {
        // ✅ Check if assignment question paper exists
        boolean exists = questionPaperRepository
                .findByCourse_CourseNameAndIsAssignmentTrueAndAssignmentNumber(courseName, assignmentNo)
                .isPresent();

        if (!exists) {
            throw new BadRequestException("Assignment not created yet , upload rejected");

        }


        Course course = courseRepository.findByCourseName(courseName)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Enrolment enrolment = student.getEnrolments().stream()
                .filter(e -> e.getCourse().getId().equals(course.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Enrolment not found"));

        // Find the assignment submission for the given assignment number
        AssignmentSubmission assignmentSubmission = enrolment.getAssignments().stream()
                .filter(a -> a.getAssignmentNumber() != null
                        && Integer.parseInt(a.getAssignmentNumber()) == assignmentNo)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Assignment submission not found"));

        String answerText = assignmentSubmission.getAssignmentSheetText();

        // Use questionPaperRepository to fetch the assignment paper
        QuestionPaper paper = questionPaperRepository
                .findByCourse_CourseNameAndIsAssignmentTrueAndAssignmentNumber(courseName, assignmentNo)
                .orElseThrow(() -> new BadRequestException("Assignment paper not found"));

        List<EvaluationResponseDto> dtos = evaluate(answerText, paper, enrolment, AnswerSheetType.ASSIGNMENT,assignmentSubmission);

        double total = dtos.stream().mapToDouble(EvaluationResponseDto::getMarksObtained).sum();

        // Store marks on the assignment submission
        assignmentSubmission.setMarks((double) total);

        // Save changes cascading through enrolment
        enrolmentRepository.save(enrolment);

        return (double) total;
    }
    private List<EvaluationResponseDto> evaluate(String answerSheetText,
                                                 QuestionPaper paper,
                                                 Enrolment enrolment,
                                                 AnswerSheetType type,
                                                 AssignmentSubmission assignmentSubmission) {

        Map<String, String> answerMap = parseAnswerSheetText(answerSheetText);

        // Clear previous scores of the same type
        if (type == AnswerSheetType.ASSIGNMENT && assignmentSubmission != null) {
            assignmentSubmission.getAnswerScores().clear();
        } else {
            enrolment.getAnswerScores().removeIf(score -> score.getType() == type);
        }

        List<EvaluationResponseDto> responseList = new ArrayList<>();

        for (Question question : paper.getQuestions()) {
            String qNo = question.getQuestionNumber();
            String studentAnswer = answerMap.getOrDefault("Ans" + qNo, "");
            String correctAnswer = question.getAnswerKey() != null ? question.getAnswerKey().getCorrectAnswer() : "";
            int questionMarks = question.getMarks().intValue();

            CompareAnswersResponse response = callScoringApi(correctAnswer, studentAnswer, questionMarks);
            String feedback = FeedbackGenerator.generateFeedback(response.getEntailment(),response.getNeutral(),response.getContradiction());

            AnswerScore answerScore = AnswerScore.builder()
                    .answerLabel(qNo)
                    .obtainedMarks(response.getScore())
                    .totalMarks(questionMarks)
                    .answerText(studentAnswer)
                    .type(type)
                    .enrolment(enrolment)
                    .assignmentSubmission(assignmentSubmission)
                    .feedback(feedback)
                    .build();


            // Add to correct location
            if (type == AnswerSheetType.ASSIGNMENT && assignmentSubmission != null) {
                assignmentSubmission.getAnswerScores().add(answerScore);
            } else {
                enrolment.getAnswerScores().add(answerScore);
            }

            responseList.add(EvaluationResponseDto.builder()
                    .questionNumber(qNo)
                    .teacherAnswer(correctAnswer)
                    .studentAnswer(studentAnswer)
                    .marksObtained(response.getScore())
                    .totalMarks(questionMarks)
                    .build());
        }

        return responseList;
    }


    private Map<String, String> parseAnswerSheetText(String text) {
        if (text == null || text.trim().isEmpty()) return Collections.emptyMap();

        // We'll store in insertion order (LinkedHashMap) to keep question order intact
        Map<String, String> result = new LinkedHashMap<>();

        Pattern answerStartPattern = Pattern.compile("(?i)[^a-zA-Z0-9]*?(ans|ams|axs|ars)[\\s:!.-]*(\\d+)\\s*(.*)?");


        String currentKey = null;
        StringBuilder currentAnswer = new StringBuilder();

        String[] lines = text.split("\\r?\\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            Matcher matcher = answerStartPattern.matcher(line);
            if (matcher.matches()) {
                // Save previous answer
                if (currentKey != null && currentAnswer.length() > 0) {
                    result.put(currentKey, currentAnswer.toString().trim());
                    currentAnswer.setLength(0);
                }

                // Start new answer
                String number = matcher.group(2);
                currentKey = "Ans" + number;

                String inlineAnswer = matcher.group(3);
                if (inlineAnswer != null && !inlineAnswer.isBlank()) {
                    currentAnswer.append(inlineAnswer.trim()).append(" ");
                }

            } else {
                if (currentKey != null) {
                    currentAnswer.append(line).append(" ");
                }
            }
        }

        // Save the final answer
        if (currentKey != null && currentAnswer.length() > 0) {
            result.put(currentKey, currentAnswer.toString().trim());
        }

        // Sort by numeric suffix of the key (e.g., "Ans1", "Ans2", ...)
        return result.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> Integer.parseInt(e.getKey().substring(3))))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldVal, newVal) -> oldVal,
                        LinkedHashMap::new
                ));
    }


    public List<AnswerScoreDto> viewMidtermScores(String studentUsername, String courseName) {
        Student student = studentRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Course course = courseRepository.findByCourseName(courseName)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Enrolment enrolment = student.getEnrolments().stream()
                .filter(e -> e.getCourse().getId().equals(course.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Enrolment not found"));

        return enrolment.getAnswerScores().stream()
                .filter(score -> score.getType() == AnswerSheetType.MIDTERM)
                .map(score -> AnswerScoreDto.builder()
                        .answerLabel(score.getAnswerLabel())
                        .obtainedMarks(score.getObtainedMarks())
                        .totalMarks(score.getTotalMarks())
                        .answerText(score.getAnswerText())
                        .type(score.getType())
                        .feedback(score.getFeedback())
                        .build())
                .toList();
    }


    public List<AnswerScoreDto> viewEndtermScores(String studentUsername, String courseName) {
        Student student = studentRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Course course = courseRepository.findByCourseName(courseName)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Enrolment enrolment = student.getEnrolments().stream()
                .filter(e -> e.getCourse().getId().equals(course.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Enrolment not found"));

        return enrolment.getAnswerScores().stream()
                .filter(score -> score.getType() == AnswerSheetType.ENDTERM)
                .map(score -> AnswerScoreDto.builder()
                        .answerLabel(score.getAnswerLabel())
                        .obtainedMarks(score.getObtainedMarks())
                        .totalMarks(score.getTotalMarks())
                        .answerText(score.getAnswerText())
                        .type(score.getType())
                        .feedback(score.getFeedback())
                        .build())
                .toList();
    }

    public List<AnswerScoreDto> viewAssignmentScores(String studentUsername, String courseName, int assignmentNo) {
        Student student = studentRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Course course = courseRepository.findByCourseName(courseName)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Enrolment enrolment = student.getEnrolments().stream()
                .filter(e -> e.getCourse().getId().equals(course.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Enrolment not found"));

        AssignmentSubmission assignmentSubmission = enrolment.getAssignments().stream()
                .filter(a -> a.getAssignmentNumber() != null
                        && Integer.parseInt(a.getAssignmentNumber()) == assignmentNo)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Assignment submission not found"));

        return assignmentSubmission.getAnswerScores().stream()
                .map(score -> AnswerScoreDto.builder()
                        .answerLabel(score.getAnswerLabel())
                        .obtainedMarks(score.getObtainedMarks())
                        .totalMarks(score.getTotalMarks())
                        .answerText(score.getAnswerText())
                        .type(score.getType())
                        .feedback(score.getFeedback())
                        .build())
                .toList();
    }

    public CompareAnswersResponse callScoringApi(String correctAnswer, String studentAnswer, int questionMarks) {
        if (studentAnswer == null || studentAnswer.trim().isEmpty()) {
            CompareAnswersResponse emptyResponse = new CompareAnswersResponse();
            emptyResponse.setScore(0.0);
            emptyResponse.setEntailment(0.0);
            emptyResponse.setNeutral(0.0);
            emptyResponse.setContradiction(0.0);
            return emptyResponse;
        }

        try {
            CompareAnswersRequest requestBody = new CompareAnswersRequest();
            requestBody.setTeacher_answer(correctAnswer);
            requestBody.setStudent_answer(studentAnswer);
            requestBody.setTotal_marks(questionMarks);

            CompareAnswersResponse response = WebClient.create()
                    .post()
                    .uri(scoringApiUrl)  // ✅ using your variable
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(requestBody))
                    .retrieve()
                    .bodyToMono(CompareAnswersResponse.class)
                    .timeout(Duration.ofSeconds(20))
                    .block();

            if (response == null) {
                throw new RuntimeException("Scoring API returned no response");
            }

            return response;

        } catch (Exception e) {
            if (isTimeoutException(e)) {
                throw new RuntimeException("Scoring API timed out after 20 seconds", e);
            }
            throw new RuntimeException("Failed to call Scoring API: " + e.getMessage(), e);
        }
    }

    private boolean isTimeoutException(Throwable e) {
        while (e != null) {
            if (e instanceof java.util.concurrent.TimeoutException ||
                    e instanceof java.net.SocketTimeoutException) return true;
            e = e.getCause();
        }
        return false;
    }

    public List<AnswerScoreDto> viewMidtermRawAnswers(String studentUsername, String courseName) {
        Student student = studentRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Enrolment enrolment = student.getEnrolments().stream()
                .filter(e -> e.getCourse().getCourseName().equals(courseName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Enrolment for course not found"));
        return parseRawAnswers(enrolment.getMidtermAnswerSheetText(), AnswerSheetType.MIDTERM);
    }

    public List<AnswerScoreDto> viewEndtermRawAnswers(String studentUsername, String courseName) {
        Student student = studentRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Enrolment enrolment = student.getEnrolments().stream()
                .filter(e -> e.getCourse().getCourseName().equals(courseName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Enrolment for course not found"));
        return parseRawAnswers(enrolment.getEndtermAnswerSheetText(), AnswerSheetType.ENDTERM);
    }

    public List<AnswerScoreDto> viewAssignmentRawAnswers(String studentUsername, String courseName, int assignmentNo) {
        Student student = studentRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Enrolment enrolment = student.getEnrolments().stream()
                .filter(e -> e.getCourse().getCourseName().equals(courseName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Enrolment for course not found"));
        AssignmentSubmission submission = enrolment.getAssignments().stream()
                .filter(a -> a.getAssignmentNumber().equals(String.valueOf(assignmentNo)))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        return parseRawAnswers(submission.getAssignmentSheetText(), AnswerSheetType.ASSIGNMENT);
    }

    private List<AnswerScoreDto> parseRawAnswers(String rawText, AnswerSheetType type) {
        if (rawText == null || rawText.isBlank()) return List.of();

        Map<String, String> answerMap = parseAnswerSheetText(rawText); // same method from earlier
        return answerMap.entrySet().stream()
                .map(entry -> AnswerScoreDto.builder()
                        .answerLabel(entry.getKey())
                        .answerText(entry.getValue())
                        .obtainedMarks(null)
                        .totalMarks(null)
                        .type(type)
                        .build())
                .toList();
    }

    @Async("externalTaskExecutor")
    @Transactional
    public void bulkEvaluateMidtermAsync(String courseName, String teacherUsername) {
        List<Enrolment> enrolments = enrolmentRepository.findByCourse_CourseName(courseName);
        int total = enrolments.size();
        int completed = 0;

        for (Enrolment enrolment : enrolments) {
            String studentUsername = enrolment.getStudent().getUsername();
            String rollNo = enrolment.getStudent().getRollNo();
            try {
                Student student = enrolment.getStudent();
                List<EvaluationResponseDto> result = evaluateMidterm(student, courseName);
                double totalMarks = result.stream().mapToDouble(EvaluationResponseDto::getMarksObtained).sum();

                Map<String, Object> payload = new HashMap<>();
                payload.put("type", "MIDTERM_EVALUATION_SUCCESS");
                payload.put("message", "✅ Midterm evaluated.");
                payload.put("studentUsername", rollNo);
                payload.put("courseName", courseName);
                payload.put("totalMarks", totalMarks);
                payload.put("collegeName", student.getCollege().getName());
                payload.put("departmentName", student.getDepartment().getName());
                payload.put("semester", student.getSemester());

                completed++;
                payload.put("progress", (completed * 100) / total);
                notifyProgress(teacherUsername, student.getRollNo(), courseName, completed, total, "✅ Evaluation Success");
                notifyStructuredClient2(teacherUsername, payload);

            } catch (Exception e) {
                completed++;
                Map<String, Object> payload = new HashMap<>();
                payload.put("type", "MIDTERM_EVALUATION_FAILURE");
                payload.put("message", "❌ Failed: " );
                payload.put("studentUsername",rollNo );
                payload.put("courseName", courseName);
                payload.put("progress", (completed * 100) / total);
                notifyProgress(teacherUsername, enrolment.getStudent().getRollNo(), courseName, completed, total, "❌ Evaluation Failed");
                notifyStructuredClient2(teacherUsername, payload);
            }
        }


        messagingTemplate.convertAndSend("/topic/evaluations/" + teacherUsername, Map.of(
                "type", "BULK_EVALUATION_COMPLETE",
                "sheetType", "MIDTERM",
                "courseName", courseName
        ));
    }

    @Async("externalTaskExecutor")
    @Transactional
    public void bulkEvaluateEndtermAsync(String courseName, String teacherUsername) {
        List<Enrolment> enrolments = enrolmentRepository.findByCourse_CourseName(courseName);
        int total = enrolments.size();
        int completed = 0;

        for (Enrolment enrolment : enrolments) {
            String studentUsername = enrolment.getStudent().getUsername();
            String rollNo = enrolment.getStudent().getRollNo();

            try {
                Student student = enrolment.getStudent();
                List<EvaluationResponseDto> result = evaluateEndterm(student, courseName);
                double totalMarks = result.stream().mapToDouble(EvaluationResponseDto::getMarksObtained).sum();

                Map<String, Object> payload = new HashMap<>();
                payload.put("type", "ENDTERM_EVALUATION_SUCCESS");
                payload.put("message", "✅ Endterm evaluated.");
                payload.put("studentUsername", rollNo);
                payload.put("courseName", courseName);
                payload.put("totalMarks", totalMarks);
                payload.put("collegeName", student.getCollege().getName());
                payload.put("departmentName", student.getDepartment().getName());
                payload.put("semester", student.getSemester());

                completed++;
                payload.put("progress", (completed * 100) / total);
                notifyProgress(teacherUsername, rollNo, courseName, completed, total, "✅ Evaluation Success");
                notifyStructuredClient2(teacherUsername, payload);
            } catch (Exception e) {
                completed++;
                Map<String, Object> payload = new HashMap<>();
                payload.put("type", "ENDTERM_EVALUATION_FAILURE");
                payload.put("message", "❌ Failed: " + e.getMessage());
                payload.put("studentUsername", rollNo);
                payload.put("courseName", courseName);
                payload.put("progress", (completed * 100) / total);
                notifyProgress(teacherUsername, rollNo, courseName, completed, total, "❌ Evaluation Failed");
                notifyStructuredClient2(teacherUsername, payload);
            }
        }

        messagingTemplate.convertAndSend("/topic/evaluations/" + teacherUsername, Map.of(
                "type", "BULK_EVALUATION_COMPLETE",
                "sheetType", "ENDTERM",
                "courseName", courseName
        ));
    }
    @Async("externalTaskExecutor")
    @Transactional
    public void bulkEvaluateAssignmentAsync(String courseName, int assignmentNumber, String teacherUsername) {
        List<Enrolment> enrolments = enrolmentRepository.findByCourse_CourseName(courseName);
        int total = enrolments.size();
        int completed = 0;

        for (Enrolment enrolment : enrolments) {
            String studentUsername = enrolment.getStudent().getUsername();
            String rollNo = enrolment.getStudent().getRollNo();

            try {
                Student student = enrolment.getStudent();
                Double marks = evaluateAssignment(student, courseName, assignmentNumber);

                Map<String, Object> payload = new HashMap<>();
                payload.put("type", "ASSIGNMENT_EVALUATION_SUCCESS");
                payload.put("message", "✅ Assignment evaluated.");
                payload.put("studentUsername", rollNo);
                payload.put("courseName", courseName);
                payload.put("assignmentNumber", assignmentNumber);
                payload.put("totalMarks", marks);
                payload.put("collegeName", student.getCollege().getName());
                payload.put("departmentName", student.getDepartment().getName());
                payload.put("semester", student.getSemester());

                completed++;
                payload.put("progress", (completed * 100) / total);
                notifyProgress(teacherUsername, rollNo, courseName, completed, total, "✅ Evaluation Success");
                notifyStructuredClient2(teacherUsername, payload);
            } catch (Exception e) {
                completed++;
                Map<String, Object> payload = new HashMap<>();
                payload.put("type", "ASSIGNMENT_EVALUATION_FAILURE");
                payload.put("message", "❌ Failed: " + e.getMessage());
                payload.put("studentUsername", rollNo);
                payload.put("courseName", courseName);
                payload.put("assignmentNumber", assignmentNumber);
                payload.put("progress", (completed * 100) / total);
                notifyProgress(teacherUsername, rollNo, courseName, completed, total, "❌ Evaluation Failed");
                notifyStructuredClient2(teacherUsername, payload);
            }
        }

        messagingTemplate.convertAndSend("/topic/evaluations/" + teacherUsername, Map.of(
                "type", "BULK_EVALUATION_COMPLETE",
                "sheetType", "ASSIGNMENT",
                "assignmentNumber", assignmentNumber,
                "courseName", courseName
        ));
    }

    private void notifyStructuredClient2(String teacherUsername, Map<String, Object> payload) {

        messagingTemplate.convertAndSend("/topic/evaluations/" + teacherUsername, payload);
    }

    private void notifyProgress(String teacherUsername, String rollNo, String courseName, int completed, int total, String statusMessage) {
        messagingTemplate.convertAndSend("/topic/teacher/" + teacherUsername, Map.of(
                "type", "BULK_EVALUATE_PROGRESS",
                "rollNo", rollNo,
                "courseName", courseName,
                "status", statusMessage,
                "progress", (completed * 100) / total
        ));
    }




}
