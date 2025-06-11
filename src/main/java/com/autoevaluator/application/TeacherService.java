package com.autoevaluator.application;

import com.autoevaluator.domain.dto.*;
import com.autoevaluator.domain.dto.QuestionPaperRequest;
import com.autoevaluator.domain.entity.*;
import com.autoevaluator.domain.exception.BadRequestException;
import com.autoevaluator.domain.models.UserPrincipal;
import com.autoevaluator.domain.repositories.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TeacherService {
    @Value("${file.upload-dir}")
    private String fileUploadDir;
    private final CourseRepository courseRepository;
    private final AppUserRepository appUserRepository;
    private final QuestionPaperRepository questionPaperRepository;
    private final QuestionRepository questionRepository;
    private final StudentRepository studentRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final TeacherRepository teacherRepository;
    private final CollegeRepository collegeRepository;
    private final DepartmentRepository departmentRepository;

    private final OcrClient ocrClient;

    @Autowired
    public TeacherService(CourseRepository courseRepository,
                          AppUserRepository appUserRepository,
                          QuestionPaperRepository questionPaperRepository,
                          QuestionRepository questionRepository,
                          StudentRepository studentRepository,
                          EnrolmentRepository enrolmentRepository,
                          TeacherRepository teacherRepository,
                          CollegeRepository collegeRepository,
                          DepartmentRepository departmentRepository,
                          OcrClient ocrClient) {
        this.courseRepository = courseRepository;
        this.appUserRepository = appUserRepository;
        this.questionPaperRepository = questionPaperRepository;
        this.questionRepository = questionRepository;
        this.studentRepository = studentRepository;
        this.enrolmentRepository = enrolmentRepository;
        this.teacherRepository = teacherRepository;
        this.collegeRepository = collegeRepository;
        this.departmentRepository = departmentRepository;
        this.ocrClient = ocrClient;
    }
    public List<TeacherDTO> getAllTeachersExceptCurrent() {
        AppUser teacherCurrent = getCurrentUser();
        String username = teacherCurrent.getUsername();
        List<Teacher> allTeachers = teacherRepository.findAll();

        return allTeachers.stream()
                .filter(teacher -> !teacher.getUsername().equals(username))
                .map(teacher -> {
                    TeacherDTO dto = new TeacherDTO();
                    dto.setUsername(teacher.getUsername());
                    dto.setName(teacher.getName());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // service/TeacherService.java
    public List<CourseDTO> getCoursesByTeacherUsername(String username) {
        if (username == null)
            throw new BadRequestException("Username can't be empty");

        username = username.toLowerCase();

        String finalUsername = username;
        Teacher teacher = teacherRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Teacher not found with username: " + finalUsername));

        String departmentName = teacher.getDepartment().getName();
        String collegeName = teacher.getCollege().getName();
        System.out.println(collegeName);
        return teacher.getCourses().stream()
                .map(course -> new CourseDTO(
                        course.getCourseName(),
                        course.getCourseCode(),
                        course.getCourseCredits(),
                        course.getSemester(),
                        departmentName,
                        collegeName
                ))
                .collect(Collectors.toList());
    }




    // Method to check if the current user is in the shared list of the question paper
    private void validateSharedWith(Long paperId) {
        QuestionPaper questionPaper = questionPaperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("Question Paper not found with ID: " + paperId));

        AppUser currentUser = getCurrentUser();

        // Check if the current user is in the sharedWith list of the paper
        boolean isSharedWithUser = questionPaper.getSharedWith()
                .stream()
                .anyMatch(user -> user.getUsername().equals(currentUser.getUsername()));

        if (!isSharedWithUser) {
            throw new RuntimeException("You are not authorized to perform this operation on this question paper.");
        }
    }

    // Create a new Question Paper
    public Long createQuestionPaper(QuestionPaperRequest questionPaperRequest) {
        AppUser currentUser = getCurrentUser();

        Course course = courseRepository.findByCourseName(questionPaperRequest.getCourseName())
                .orElseThrow(() -> new RuntimeException("Course not found: " + questionPaperRequest.getCourseName()));

        QuestionPaper questionPaper = new QuestionPaper();
        questionPaper.setSemester(questionPaperRequest.getSemester());
        questionPaper.setDepartmentName(questionPaperRequest.getDepartmentName());
        questionPaper.setCollegeName(questionPaperRequest.getCollegeName());
        questionPaper.setCourse(course);
        questionPaper.setCourseName(course.getCourseName());
        questionPaper.setCreatedBy(currentUser);
        questionPaper.setCreatedAt(String.valueOf(LocalDateTime.now()));
        questionPaper.setTotalMarks(questionPaperRequest.getTotalMarks());

        // Set isAssignment true only if assignmentNumber is present in request
        if (questionPaperRequest.getAssignmentNumber() != null) {
            questionPaper.setIsAssignment(true);
            questionPaper.setAssignmentNumber(questionPaperRequest.getAssignmentNumber());
        }

        // You may still want to set midterm or endterm flags based on questionPaperFor if needed
        // Otherwise ignore those here as per your instruction

        List<AppUser> sharedWithUsers = new ArrayList<>();
        sharedWithUsers.add(currentUser);
        questionPaper.setSharedWith(sharedWithUsers);

        QuestionPaper savedPaper = questionPaperRepository.save(questionPaper);

        return savedPaper.getId();
    }


    // Get Question Paper by ID
    public QuestionPaperResponse getQuestionPaperById(Long id) {
        // Check if the current user has permission to view this paper
        validateSharedWith(id);

        // Retrieve the question paper by ID
        QuestionPaper questionPaper = questionPaperRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question Paper not found with ID: " + id));

        // Map QuestionPaper -> QuestionPaperResponse manually
        QuestionPaperResponse response = new QuestionPaperResponse();
        response.setId(questionPaper.getId());
        response.setSemester(questionPaper.getSemester());
        response.setDepartmentName(questionPaper.getDepartmentName());
        response.setCollegeName(questionPaper.getCollegeName());
        response.setCourseName(questionPaper.getCourseName());
        response.setCourseCode(questionPaper.getCourse().getCourseCode());
        if (questionPaper.isMidterm())
            response.setType("Midterm");
        System.out.println(response.getType());
        if (questionPaper.isEndterm())
            response.setType("Endterm");
        if (questionPaper.getIsAssignment()!=null && questionPaper.getIsAssignment() == true)
            response.setType("Assignment");
        System.out.println(response.getType());
       // response.setCourseName(questionPaper.getCourse().getCoursename());
        response.setCreator(questionPaper.getCreatedBy().getUsername());
        response.setQuestions(
                questionPaper.getQuestions()
                        .stream()
                        .map(q -> {
                            QuestionAnswerDTO qa = new QuestionAnswerDTO();
                            System.out.println("Q" + q.getQuestionNumber() + " Instructions: " + q.getInstructions());
                            qa.setQuestionText(q.getText());
                            qa.setCorrectAnswer(q.getAnswerKey() != null ? q.getAnswerKey().getCorrectAnswer() : null);
                            qa.setMarks(q.getMarks());
                            qa.setId(q.getId());
                            qa.setQuestionNumber(q.getQuestionNumber()); // FIXED LINE
                            qa.setInstructions(q.getInstructions());
                            return qa;
                        })

                        .toList()
        );
        System.out.println(response.getType());

        return response;
    }

    // Add Question to Paper
    @Transactional
    public void addQuestionToPaper(Long paperId, QuestionRequest questionRequest) {
        validateSharedWith(paperId);

        QuestionPaper questionPaper = questionPaperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("Question Paper not found with ID: " + paperId));

        boolean questionNumberExists = questionPaper.getQuestions().stream()
                .anyMatch(q -> q.getQuestionNumber().equals(questionRequest.getQuestionNumber()));

        if (questionNumberExists) {
            throw new BadRequestException("Question number already exists in the question paper.");
        }

        double usedMarks = questionPaper.getQuestions().stream()
                .mapToDouble(q -> q.getMarks() != null ? q.getMarks() : 0.0)
                .sum();

        if (usedMarks + questionRequest.getMarks() > questionPaper.getTotalMarks()) {
            throw new BadRequestException("Adding this question exceeds the total allowed marks of the question paper.");
        }

        Question question = new Question();
        question.setQuestionNumber(questionRequest.getQuestionNumber());
        question.setText(questionRequest.getText());
        question.setMarks(questionRequest.getMarks());
        question.setInstructions(questionRequest.getInstructions());
        question.setQuestionPaper(questionPaper);

        if (questionRequest.getCorrectAnswer() != null) {
            AnswerKey answerKey = new AnswerKey();
            answerKey.setCorrectAnswer(questionRequest.getCorrectAnswer());
            answerKey.setQuestion(question);
            question.setAnswerKey(answerKey);
        }

        questionPaper.getQuestions().add(question);
        questionPaperRepository.save(questionPaper);
    }

    @Transactional
    public void updateQuestionInPaper(Long paperId, Long questionId, QuestionRequest questionRequest) {
        validateSharedWith(paperId);

        QuestionPaper paper = questionPaperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("Question Paper not found with ID: " + paperId));

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found with ID: " + questionId));

        if (!question.getQuestionPaper().getId().equals(paperId)) {
            throw new RuntimeException("Question does not belong to this paper.");
        }

        if (!question.getQuestionNumber().equals(questionRequest.getQuestionNumber())) {
            boolean questionNumberExists = paper.getQuestions().stream()
                    .anyMatch(q -> !q.getId().equals(questionId) &&
                            q.getQuestionNumber().equals(questionRequest.getQuestionNumber()));
            if (questionNumberExists) {
                throw new RuntimeException("Question number already exists in the question paper.");
            }
        }

        // Validate that updated marks do not exceed total paper marks
        double totalUsedMarksExcludingCurrent = paper.getQuestions().stream()
                .filter(q -> !q.getId().equals(questionId))
                .mapToDouble(q -> q.getMarks() != null ? q.getMarks() : 0.0)
                .sum();

        if (totalUsedMarksExcludingCurrent + questionRequest.getMarks() > paper.getTotalMarks()) {
            throw new BadRequestException("Updating this question exceeds the total allowed marks of the question paper.");
        }

        question.setText(questionRequest.getText());
        question.setMarks(questionRequest.getMarks());
        question.setQuestionNumber(questionRequest.getQuestionNumber());
        question.setInstructions(questionRequest.getInstructions());

        if (questionRequest.getCorrectAnswer() != null) {
            AnswerKey answerKey = question.getAnswerKey();
            if (answerKey == null) {
                answerKey = new AnswerKey();
                answerKey.setQuestion(question);
                question.setAnswerKey(answerKey);
            }
            answerKey.setCorrectAnswer(questionRequest.getCorrectAnswer());
        }

        questionRepository.save(question);
    }


    // Delete Question from Paper
    @Transactional
    public void deleteQuestionFromPaper(Long paperId, Long questionId) {
        // Validate that the current user can delete the question
        validateSharedWith(paperId);

        // Find the Question Paper by its ID
        QuestionPaper questionPaper = questionPaperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("Question Paper not found with ID: " + paperId));

        // Find the Question by its ID (use the appropriate repository for Question)
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found with ID: " + questionId));

        // Check if the question belongs to the paper
        if (!question.getQuestionPaper().getId().equals(paperId)) {
            throw new RuntimeException("Question does not belong to the specified Question Paper.");
        }

        // Remove the question from the paper's questions list
        questionPaper.getQuestions().remove(question);

        // Save the updated Question Paper after removing the question
        questionPaperRepository.save(questionPaper);

        // Optionally, delete the question itself (if you want to remove the entity entirely)
        questionRepository.delete(question);
    }

    public void deleteQuestionPaper(Long paperId) {
        // Validate that the current user is authorized to delete the paper
        validateSharedWith(paperId);

        // Find the question paper by ID
        QuestionPaper questionPaper = questionPaperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("Question Paper not found with ID: " + paperId));

        // Delete the question paper from the repository
        questionPaperRepository.delete(questionPaper);
    }



    public List<QuestionPaperResponse> getAllQuestionPapersByTeacher(String courseName, Integer semester) {
        AppUser teacher = getCurrentUser();
        if (teacher == null) {
            throw new RuntimeException("Teacher not found");
        }

        List<QuestionPaper> questionPapers = questionPaperRepository.findByCreatedBy(teacher);

        Stream<QuestionPaper> filteredStream = questionPapers.stream();

        if (courseName != null && !courseName.isEmpty()) {
            filteredStream = filteredStream.filter(qp ->
                    qp.getCourse() != null &&
                            qp.getCourse().getCourseName().equalsIgnoreCase(courseName));
        }

        if (semester != null) {
            filteredStream = filteredStream.filter(qp -> {
                try {
                    return Integer.valueOf(qp.getSemester()) == semester;
                } catch (NumberFormatException e) {
                    return false;
                }
            });
        }

        return filteredStream.map(questionPaper -> {
            QuestionPaperResponse response = new QuestionPaperResponse();
            response.setId(questionPaper.getId());
            response.setSemester(questionPaper.getSemester());
            response.setCourseName(questionPaper.getCourse().getCourseName());
            response.setDepartmentName(questionPaper.getDepartmentName());
            response.setCollegeName(questionPaper.getCollegeName());
            response.setQuestions(null);
            response.setTotalMarks(questionPaper.getTotalMarks());
            if (questionPaper.getAssignmentNumber()!= null)
                response.setAssignmentNumber(questionPaper.getAssignmentNumber());
            if (questionPaper.isMidterm()) {
                response.setType("Midterm");
            } else if (questionPaper.isEndterm()) {
                response.setType("Endterm");
            } else if (questionPaper.getIsAssignment() == true) {
                response.setType("Assignment");
            } else {
                response.setType(null);
            }
            try {
                response.setLastModifiedAt(LocalDateTime.parse(questionPaper.getCreatedAt()));
            } catch (Exception e) {
                System.out.println(e);
            }

            return response;
        }).collect(Collectors.toList());
    }


    // Helper method to get the current authenticated username

    @Transactional(readOnly = true)
    public List<StudentResponse> getStudentDashboardInfo(String collegeName, String departmentName, int semester, String courseName) {
        College college = collegeRepository.findByName(collegeName)
                .orElseThrow(() -> new RuntimeException("College not found"));

        Department department = departmentRepository.findByName(departmentName)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        Course course = courseRepository.findByCourseName(courseName)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        List<Student> students = studentRepository.findByCollegeDepartmentSemesterAndCourse(
                college, department, semester, course
        );

        return students.stream()
                .map(student -> {
                    StudentResponse response = new StudentResponse();

                    response.setId(student.getId());
                    response.setRollno(student.getRollNo());
                    response.setName(student.getName());
                    response.setUsername(student.getUsername());

                    Enrolment enrolment = student.getEnrolments().stream()
                            .filter(e -> e.getCourse().getCourseName().equals(courseName))
                            .findFirst()
                            .orElse(null);

                    if (enrolment != null) {
                        if (StringUtils.hasText(enrolment.getMidtermAnswerSheetText())) {
                            response.setMidtermAnswerSheetUrl("Some url");
                        }
                        if (StringUtils.hasText(enrolment.getEndtermAnswerSheetText())) {
                            response.setEndtermAnswerSheetUrl("Some url");
                        }
                        response.setEndtermAnswerSheetUrl(enrolment.getEndtermAnswerSheetUrl());
                        response.setMidtermMarks(enrolment.getMidtermMarks());
                        response.setEndtermMarks(enrolment.getEndtermMarks());

                        // ✅ Map assignment submissions
                        List<StudentResponse.AssignmentData> assignmentDataList = enrolment.getAssignments().stream()
                                .map(a -> {
                                    StudentResponse.AssignmentData assignmentData = new StudentResponse.AssignmentData();
                                    assignmentData.setAssignmentTitle(a.getAssignmentNumber());
                                    assignmentData.setAnswerSheetUrl(a.getAnswerSheetUrl());
                                    assignmentData.setMarks(a.getMarks());
                                    return assignmentData;
                                })
                                .collect(Collectors.toList());

                        response.setAssignments(assignmentDataList);
                    }

                    return response;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public String uploadAnswerSheet(String studentUsername, String courseName, MultipartFile file, AnswerSheetType type, String assignmentNumber) {
        try {
            Student student = studentRepository.findByUsername(studentUsername)
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            Enrolment enrolment = student.getEnrolments().stream()
                    .filter(e -> e.getCourse().getCourseName().equals(courseName))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Enrolment for course not found"));

            // 🔥 OCR call
            String extractedText = ocrClient.extractText(file);

            switch (type) {
                case MIDTERM -> {
                    enrolment.setMidtermAnswerSheetUrl("Some sheet url"); // or leave it unchanged
                    enrolment.setMidtermAnswerSheetText(extractedText);
                }
                case ENDTERM -> {
                    enrolment.setEndtermAnswerSheetUrl("some sheet url"); // or leave it unchanged
                    enrolment.setEndtermAnswerSheetText(extractedText);
                }
                case ASSIGNMENT -> {
                    AssignmentSubmission submission = new AssignmentSubmission();
                    submission.setAnswerSheetUrl("some sheet url"); // or skip setting it
                    submission.setAssignmentSheetText(extractedText);
                    submission.setAssignmentNumber(assignmentNumber);
                    submission.setEnrolment(enrolment);
                    enrolment.getAssignments().add(submission);
                }
            }

            enrolmentRepository.save(enrolment);
            return "Text extracted and saved successfully.";

        } catch (Exception e) {
            throw new RuntimeException("Error uploading file: " + e.getMessage(), e);
        }
    }

//
//    public String uploadAnswerSheet(String studentUsername, String courseName, MultipartFile file, AnswerSheetType type, String assignmentNumber) {
//        try {
//            Student student = studentRepository.findByUsername(studentUsername)
//                    .orElseThrow(() -> new RuntimeException("Student not found"));
//
//            Enrolment enrolment = student.getEnrolments().stream()
//                    .filter(e -> e.getCourse().getCourseName().equals(courseName))
//                    .findFirst()
//                    .orElseThrow(() -> new RuntimeException("Enrolment for course not found"));
//
//            String folder = switch (type) {
//                case MIDTERM -> "midterms";
//                case ENDTERM -> "endterms";
//                case ASSIGNMENT -> "assignments";
//            };
//
//            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
//            String fullFolderPath = fileUploadDir + "/" + folder;
//            Path folderPath = Paths.get(fullFolderPath);
//            if (!Files.exists(folderPath)) {
//                Files.createDirectories(folderPath);
//            }
//
//            Path path = Paths.get(fullFolderPath, fileName);
//            Files.copy(file.getInputStream(), path);
//
//            String fileUrl = "/files/" + folder + "/" + fileName;
//
//            // 🔥 OCR call
//            String extractedText = ocrClient.extractText(file);
//            switch (type) {
//                case MIDTERM -> {
//                    enrolment.setMidtermAnswerSheetUrl(fileUrl);
//                    enrolment.setMidtermAnswerSheetText(extractedText); // ⬅️ Store text
//                }
//                case ENDTERM -> {
//                    enrolment.setEndtermAnswerSheetUrl(fileUrl);
//                    enrolment.setEndtermAnswerSheetText(extractedText); // ⬅️ Store text
//                }
//                case ASSIGNMENT -> {
//                    AssignmentSubmission submission = new AssignmentSubmission();
//                    submission.setAnswerSheetUrl(fileUrl);
//                    submission.setAssignmentSheetText(extractedText); // ⬅️ Store text
//
//                    submission.setAssignmentNumber(
//                            (assignmentNumber != null && !assignmentNumber.isBlank())
//                                    ? assignmentNumber
//                                    : "Assignment " + (enrolment.getAssignments().size() + 1)
//                    );
//                    submission.setEnrolment(enrolment);
//                    enrolment.getAssignments().add(submission);
//                }
//            }
//
//            enrolmentRepository.save(enrolment);
//            return fileUrl;
//
//        } catch (Exception e) {
//            throw new RuntimeException("Error uploading file: " + e.getMessage(), e);
//        }
//    }




    public String uploadMidtermSheet(String studentUsername, String courseName, MultipartFile file) {
        return uploadAnswerSheet(studentUsername, courseName, file, AnswerSheetType.MIDTERM,"");
    }

    public String uploadEndtermSheet(String studentUsername, String courseName, MultipartFile file) {
        return uploadAnswerSheet(studentUsername, courseName, file, AnswerSheetType.ENDTERM,"");
    }

    public String uploadAssignmentSheet(String studentUsername, String courseName, MultipartFile file, String assignmentNumber) {
        System.out.println("Upload assignemt sheet called");
        return uploadAnswerSheet(studentUsername, courseName, file, AnswerSheetType.ASSIGNMENT, assignmentNumber);
    }

    @Transactional
    public void deleteMidtermSheet(String studentUsername, String courseName) {
        Student student = studentRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Enrolment enrolment = student.getEnrolments().stream()
                .filter(e -> e.getCourse().getCourseName().equals(courseName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Enrolment for course not found"));

        // Remove MIDTERM answer scores
        enrolment.getAnswerScores().removeIf(score -> score.getType() == AnswerSheetType.MIDTERM);

        // Reset sheet URL and marks
        enrolment.setMidtermAnswerSheetUrl(null);
        enrolment.setMidtermMarks(0.0);

        enrolmentRepository.save(enrolment);
    }


    @Transactional
    public void deleteEndtermSheet(String studentUsername, String courseName) {
        Student student = studentRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Enrolment enrolment = student.getEnrolments().stream()
                .filter(e -> e.getCourse().getCourseName().equals(courseName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Enrolment for course not found"));

        // Remove ENDTERM answer scores
        enrolment.getAnswerScores().removeIf(score -> score.getType() == AnswerSheetType.ENDTERM);

        // Reset sheet URL and marks
        enrolment.setEndtermAnswerSheetUrl(null);
        enrolment.setEndtermMarks(0.0);

        enrolmentRepository.save(enrolment);
    }


    @Transactional
    public void deleteAssignmentSheet(String studentUsername, String courseName, String assignmentNumber) {
        Student student = studentRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Enrolment enrolment = student.getEnrolments().stream()
                .filter(e -> e.getCourse().getCourseName().equals(courseName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Enrolment for course not found"));

        // Find the assignment submission
        AssignmentSubmission assignmentSubmission = enrolment.getAssignments().stream()
                .filter(a -> assignmentNumber.equals(a.getAssignmentNumber()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Assignment with number " + assignmentNumber + " not found"));

        // Remove from enrolment's assignment list
        enrolment.getAssignments().remove(assignmentSubmission);

        // (Optional) If not using orphanRemoval, delete explicitly:
        // assignmentSubmissionRepository.delete(assignmentSubmission);

        enrolmentRepository.save(enrolment);
    }

    public void assignPaperAsMidterm(Long paperId) {
        QuestionPaper paper = questionPaperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("Question Paper not found"));

        if (Boolean.TRUE.equals(paper.getIsAssignment())) {
            throw new BadRequestException("Cannot assign an assignment paper as Midterm.");
        }

        Course course = paper.getCourse();

        // Unassign other midterm paper for this course
        List<QuestionPaper> coursePapers = questionPaperRepository.findByCourse(course);
        for (QuestionPaper qp : coursePapers) {
            if (qp.isMidterm()) {
                qp.setMidterm(false);
                questionPaperRepository.save(qp);
            }
        }

        paper.setMidterm(true);
        paper.setEndterm(false);
        questionPaperRepository.save(paper);
    }

    public void assignPaperAsEndterm(Long paperId) {
        QuestionPaper paper = questionPaperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("Question Paper not found"));

        if (Boolean.TRUE.equals(paper.getIsAssignment())) {
            throw new BadRequestException("Cannot assign an assignment paper as Endterm.");
        }

        Course course = paper.getCourse();

        // Unassign other endterm paper for this course
        List<QuestionPaper> coursePapers = questionPaperRepository.findByCourse(course);
        for (QuestionPaper qp : coursePapers) {
            if (qp.isEndterm()) {
                qp.setEndterm(false);
                questionPaperRepository.save(qp);
            }
        }

        paper.setEndterm(true);
        paper.setMidterm(false);
        questionPaperRepository.save(paper);
    }


//    public boolean isTeacherAssigned(String username, String departmentName, String courseName, int semester) {
//        Teacher teacher = teacherRepository.findByUsername(username)
//                .orElseThrow(() -> new RuntimeException("Teacher not found with username: " + username));
//
//        boolean isInDepartment = teacher.getDepartments().stream()
//                .anyMatch(dept -> dept.getName().equalsIgnoreCase(departmentName));
//
//        boolean isInCourseWithSemester = teacher.getCourses().stream()
//                .anyMatch(course ->
//                        course.getCourseName().equalsIgnoreCase(courseName)
//                                && course.getSemester() == semester
//                );
//
//        return isInDepartment && isInCourseWithSemester;
//    }


    public void sharePaperWithTeacher(Long paperId, String teacherUsername) {
        // Retrieve the question paper by ID
        QuestionPaper paper = questionPaperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("Question Paper not found with ID: " + paperId));

        // Find the teacher by username
        AppUser teacher = appUserRepository.findByUsername(teacherUsername);

        // Ensure the user is a Teacher
        if (!(teacher instanceof Teacher)) {
            throw new RuntimeException("The user is not a teacher.");
        }

        // Check if the paper is already shared with the teacher
        if (!paper.getSharedWith().contains(teacher)) {
            paper.getSharedWith().add(teacher);
            questionPaperRepository.save(paper);
        } else {
            throw new RuntimeException("The paper is already shared with this teacher.");
        }
    }

    public void unsharePaperWithTeacher(Long paperId, String teacherUsername) {
        // Retrieve the question paper by ID
        QuestionPaper paper = questionPaperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("Question Paper not found with ID: " + paperId));

        // Find the teacher by username (make sure to handle the Optional correctly)
        AppUser teacher = appUserRepository.findByUsername(teacherUsername);
        //.orElseThrow(() -> new RuntimeException("Teacher not found with username: " + teacherUsername));

        // Ensure the user is a Teacher
        if (!(teacher instanceof Teacher)) {
            throw new RuntimeException("The user is not a teacher.");
        }

        // Check if the paper is shared with the teacher
        if (paper.getSharedWith().contains(teacher)) {
            paper.getSharedWith().remove(teacher);
            questionPaperRepository.save(paper);
        } else {
            throw new RuntimeException("The paper is not shared with this teacher.");
        }
    }

    private AppUser getCurrentUser() {
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email = userPrincipal.getUsername();
        return appUserRepository.findByUsername(email);
    }



}

