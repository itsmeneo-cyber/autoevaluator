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
        try {
            Student student = studentRepository.findByUsername(studentUsername)
                    .orElseThrow(() -> new RuntimeException("Student not found"));
            List<EvaluationResponseDto> result = evaluateMidterm(student, courseName);
            double total = result.stream().mapToDouble(EvaluationResponseDto::getMarksObtained).sum();



            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "MIDTERM_EVALUATION_SUCCESS");
            payload.put("message", "✅ Midterm evaluated. You can now view the answer sheet.");
            payload.put("studentUsername", studentUsername);
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
            payload.put("studentUsername", studentUsername);
            payload.put("courseName", courseName);
            notifyStructuredClient(teacherUsername, payload);
        }
    }
    @Async("externalTaskExecutor")
    @Transactional
    public void evaluateEndtermAsync(String studentUsername, String courseName, String teacherUsername) {
        try {
            Student student = studentRepository.findByUsername(studentUsername)
                    .orElseThrow(() -> new RuntimeException("Student not found"));
            List<EvaluationResponseDto> result = evaluateEndterm(student, courseName);
            double total = result.stream().mapToDouble(EvaluationResponseDto::getMarksObtained).sum();



            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "ENDTERM_EVALUATION_SUCCESS");
            payload.put("message", "✅ Endterm evaluated. You can now view the answer sheet.");
            payload.put("studentUsername", studentUsername);
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
            payload.put("studentUsername", studentUsername);
            payload.put("courseName", courseName);
            notifyStructuredClient(teacherUsername, payload);
        }
    }
    @Async("externalTaskExecutor")
    @Transactional
    public void evaluateAssignmentAsync(String studentUsername, String courseName, int assignmentNo, String teacherUsername) {
        try {
            Student student = studentRepository.findByUsername(studentUsername)
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            Double score = evaluateAssignment(student, courseName, assignmentNo);


            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "ASSIGNMENT_EVALUATION_SUCCESS");
            payload.put("message", "✅ Assignment evaluated. You can now view the answer sheet.");
            payload.put("studentUsername", studentUsername);
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
            payload.put("studentUsername", studentUsername);
            payload.put("courseName", courseName);
            payload.put("assignmentNumber", assignmentNo);
            notifyStructuredClient(teacherUsername, payload);
        }
    }

    private void notifyStructuredClient(String teacherUsername, Map<String, Object> payload) {
        try {
            System.out.println("🔍 Current Thread: " + Thread.currentThread().getName());
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Delay interrupted: " + e.getMessage());
        }
        messagingTemplate.convertAndSend("/topic/evaluations/" + teacherUsername, payload);
    }

    public List<EvaluationResponseDto> evaluateMidterm(Student student, String courseName) {


        Course course = courseRepository.findByCourseName(courseName)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Enrolment enrolment = student.getEnrolments().stream()
                .filter(e -> e.getCourse().getId().equals(course.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Enrolment not found for this student and course"));

        String answerText = enrolment.getMidtermAnswerSheetText();

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
        System.out.println("Reached here ");
        System.out.println(answerText);
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

            Double obtainedMarks = callScoringApi(correctAnswer, studentAnswer, questionMarks);




            AnswerScore answerScore = AnswerScore.builder()
                    .answerLabel(qNo)
                    .obtainedMarks(obtainedMarks)
                    .totalMarks(questionMarks)
                    .answerText(studentAnswer)
                    .type(type)
                    .enrolment(enrolment)
                    .assignmentSubmission(assignmentSubmission)
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
                    .marksObtained(obtainedMarks)
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
                        .build())
                .toList();
    }

    private Double callScoringApi(String correctAnswer, String studentAnswer, int questionMarks) {
        if (studentAnswer == null || studentAnswer.trim().isEmpty()) {
            return 0.0;
        }

        CompareAnswersRequest requestBody = new CompareAnswersRequest();
        requestBody.setTeacher_answer(correctAnswer);
        requestBody.setStudent_answer(studentAnswer);
        requestBody.setTotal_marks(questionMarks);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CompareAnswersRequest> entity = new HttpEntity<>(requestBody, headers);

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<CompareAnswersResponse> response = restTemplate.exchange(
                scoringApiUrl,
                HttpMethod.POST,
                entity,
                CompareAnswersResponse.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Scoring API failed with status: " + response.getStatusCode());
        }

        CompareAnswersResponse body = response.getBody();
        if (body == null) {
            throw new RuntimeException("Scoring API response was empty");
        }

        return body.getScore();
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


}
