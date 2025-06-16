package com.autoevaluator.application;

import com.autoevaluator.domain.dto.*;
import com.autoevaluator.domain.entity.*;
import com.autoevaluator.domain.exception.BadRequestException;
import com.autoevaluator.domain.models.UserPrincipal;
import com.autoevaluator.domain.repositories.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final AdminRepository adminRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;
    private final CollegeRepository collegeRepository;
    private final AppUserRepository appUserRepository;
    private final EmailService emailService;
    private final SuperAdminRepository superAdminRepository;

    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    public AdminService(StudentRepository studentRepository, TeacherRepository teacherRepository,

                        AdminRepository adminRepository, EnrolmentRepository enrolmentRepository,
                        CourseRepository courseRepository, DepartmentRepository departmentRepository,
                        CollegeRepository collegeRepository, AppUserRepository appUserRepository, EmailService emailService, SuperAdminRepository superAdminRepository) {
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.adminRepository = adminRepository;
        this.enrolmentRepository = enrolmentRepository;
        this.courseRepository = courseRepository;
        this.departmentRepository = departmentRepository;
        this.collegeRepository = collegeRepository;
        this.appUserRepository = appUserRepository;
        this.emailService = emailService;
        this.superAdminRepository = superAdminRepository;

    }

@Transactional
public void addStudent(StudentDTO studentDTO) {
    String username = studentDTO.getUsername();
    String rollNo = studentDTO.getRollNo();

    // Validate required fields
    if (username == null || username.trim().isEmpty()) {
        throw new BadRequestException("Username (email) is required.");
    }
    if (rollNo == null || rollNo.trim().isEmpty()) {
        throw new BadRequestException("Roll number is required.");
    }

    username = username.toLowerCase();

    // Check if username already exists
    if (studentRepository.existsByUsername(username)) {
        throw new BadRequestException("A student with this email/username already exists.");
    }

    // Check if roll number already exists
    if (studentRepository.existsByRollNo(rollNo)) {
        throw new BadRequestException("A student with this roll number already exists.");
    }
    Student student = new Student();
    student.setUsername(username);
    student.setRole("ROLE_STUDENT");
    student.setCreatedAt(LocalDateTime.now());
    student.setRollNo(rollNo);
    student.setName(studentDTO.getName());
    student.setSemester(studentDTO.getSemester());
    student.setDepartmentName(studentDTO.getDepartment());

    College college = collegeRepository.findByName(studentDTO.getCollege())
            .orElseThrow(() -> new BadRequestException("College not found: " + studentDTO.getCollege()));
    student.setCollege(college);
    Department department = departmentRepository.findByName(studentDTO.getDepartment())
            .orElseThrow(()->new BadRequestException("Department does not exist"));
    student.setDepartment(department);

    student = studentRepository.save(student);

    List<Course> courses = courseRepository.findByDepartmentAndSemester(
            studentDTO.getDepartment(), studentDTO.getSemester());

    if (courses.isEmpty()) {
        throw new BadRequestException("No courses found for the given department and semester.");
    }

    for (Course course : courses) {
        Enrolment enrolment = new Enrolment();
        enrolment.setStudent(student);
        enrolment.setCourse(course);
        enrolmentRepository.save(enrolment);
    }
}


    @Transactional
    public void addTeacher(TeacherDTO teacherDTO, String collegeName, String departmentName) {
        College college = collegeRepository.findByName(collegeName)
                .orElseThrow(() -> new BadRequestException("College not found"));

        Department department = departmentRepository.findByName(departmentName)
                .orElseThrow(() -> new BadRequestException("Department not found"));
        String username = teacherDTO.getUsername();
        String registrationId = teacherDTO.getRegistrationId();

        if (username == null || username.trim().isEmpty()) {
            throw new BadRequestException("Username (email) is required.");
        }
        if (registrationId == null || registrationId.trim().isEmpty()) {
            throw new BadRequestException("Registration ID is required.");
        }
        username = username.toLowerCase();
        Optional<AppUser> existingByUsername = Optional.ofNullable(appUserRepository.findByUsername(username));
        if (existingByUsername.isPresent()) {
            throw new BadRequestException("A user with this email already exists.");
        }
        Optional<Teacher> existingByRegId = teacherRepository.findByRegistrationId(registrationId);
        if (existingByRegId.isPresent()) {
            throw new BadRequestException("A teacher with this registration ID already exists.");
        }

        Teacher teacher = new Teacher();

        teacher.setUsername(username);
        teacher.setRegistrationId(registrationId);

        teacher.setRole("ROLE_TEACHER");
        teacher.setCreatedAt(LocalDateTime.now());

        if (teacherDTO.getName() != null && !teacherDTO.getName().isEmpty()) {
            teacher.setName(teacherDTO.getName());
        }



        teacher.setCollege(college);
        teacher.setDepartment(department);

        teacherRepository.save(teacher);
    }


    public void addAdmin(AdminDTO adminDTO) {
        // Step 1: Create Admin entity (Admin extends AppUser!)
        Admin currentAdmin = (Admin) getCurrentUser(); // Cast to Admin
        College college = currentAdmin.getCollege();

        Admin admin = new Admin();
        String userName = adminDTO.getUserName();
        if (userName == null || userName.trim().isEmpty()) {
            throw new BadRequestException("Username (email) is required.");
        }
        userName = userName.toLowerCase();
        // Set AppUser common fields
        AppUser existingUser = appUserRepository.findByUsername(userName);
        if(existingUser!=null)
            throw  new BadRequestException("User already exists");
        admin.setUsername(userName);

        // Always encode password
        admin.setCreatedAt(LocalDateTime.now());
        admin.setRole("ROLE_ADMIN");
        admin.setPrimary(false);
        admin.setName(admin.getName());
        admin.setCollege(college);

        adminRepository.save(admin);
    }

    @Transactional
    public void removeStudentByUsername(String username) {
        if (username == null )
            throw  new BadRequestException("Username can't be empty");
        username = username.toLowerCase();
        Student student = studentRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        studentRepository.delete(student);
    }

    @Transactional
    public void removeTeacherByUsername(String username, String collegeName, String departmentName) {
        if (username == null )
            throw  new BadRequestException("Username can't be empty");
        username = username.toLowerCase();
        Teacher teacher = teacherRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
        teacherRepository.delete(teacher);
    }

    @Transactional
    public void removeAdminByUsername(String username) {
        AppUser currentUser = getCurrentUser();
        if (username == null || username.isBlank()) {
            throw new BadRequestException("Username can't be empty");
        }

        username = username.toLowerCase();
        Admin adminToDelete = adminRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        // Prevent deletion of primary admin
        if (adminToDelete.isPrimary()) {
            throw new BadRequestException("Primary admin cannot be deleted.");
        }

        // Ensure admin is deleting someone from their own college
        if (currentUser instanceof Admin currentAdmin) {
            if (!adminToDelete.getCollege().getId().equals(currentAdmin.getCollege().getId())) {
                throw new BadRequestException("You cannot delete an admin from another college.");
            }
        }

        adminRepository.delete(adminToDelete);
    }


    @Transactional
    public void updateStudentByUsername(String username, StudentDTO studentDTO) {
        if (username == null)
            throw new BadRequestException("Username can't be empty");

        username = username.toLowerCase();

        String finalUsername = username;
        Student student = studentRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Student not found with username: " + finalUsername));

        // ✅ Update username if different
        if (StringUtils.hasText(studentDTO.getUsername()) &&
                !studentDTO.getUsername().equals(username)) {

            boolean usernameExists = studentRepository.existsByUsername(studentDTO.getUsername().toLowerCase());
            if (usernameExists) {
                throw new BadRequestException("A student with this email/username already exists.");
            }

            student.setUsername(studentDTO.getUsername().toLowerCase());
        }

        // ✅ Update roll number if different
        if (StringUtils.hasText(studentDTO.getRollNo()) &&
                !studentDTO.getRollNo().equals(student.getRollNo())) {

            boolean rollNoExists = studentRepository.existsByRollNo(studentDTO.getRollNo());
            if (rollNoExists) {
                throw new BadRequestException("A student with this roll number already exists.");
            }

            student.setRollNo(studentDTO.getRollNo());
        }

        // ✅ Update password if provided
        if (StringUtils.hasText(studentDTO.getPassword())) {
            student.setPassword(encoder.encode(studentDTO.getPassword()));
        }

        // ✅ Update name if provided
        if (StringUtils.hasText(studentDTO.getName())) {
            student.setName(studentDTO.getName());
        }

        // ✅ Update semester if non-zero
        if (studentDTO.getSemester() != 0) {
            student.setSemester(studentDTO.getSemester());
        }

        // ✅ Update department if provided
        if (StringUtils.hasText(studentDTO.getDepartment())) {
            Department department = departmentRepository.findByName(studentDTO.getDepartment())
                    .orElseThrow(() -> new RuntimeException("Department not found with name: " + studentDTO.getDepartment()));
            student.setDepartmentName(department.getName());
            student.setDepartment(department);
        }

        studentRepository.save(student);
    }


    @Transactional
    public void updateTeacherByUsername(String username, TeacherDTO teacherDTO, String collegeName, String departmentName) {
        if (username == null) {
            throw new BadRequestException("Username can't be empty");
        }

        username = username.toLowerCase();

        String finalUsername = username;
        Teacher teacher = teacherRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Teacher not found with username: " + finalUsername));

        // ✅ Update username if provided and different
        if (StringUtils.hasText(teacherDTO.getUsername())) {
            String newUsername = teacherDTO.getUsername().toLowerCase();

            if (!newUsername.equals(username)) {
                boolean usernameExists = teacherRepository.findByUsername(newUsername).isPresent();
                if (usernameExists) {
                    throw new BadRequestException("A teacher with this email/username already exists.");
                }
                teacher.setUsername(newUsername);
            }
        }

        // ✅ Update password if provided
        if (StringUtils.hasText(teacherDTO.getPassword())) {
            teacher.setPassword(encoder.encode(teacherDTO.getPassword()));
        }

        // ✅ Update name if provided
        if (StringUtils.hasText(teacherDTO.getName())) {
            teacher.setName(teacherDTO.getName());
        }

        // ✅ Update registration ID if provided and different
        if (StringUtils.hasText(teacherDTO.getRegistrationId()) &&
                !teacherDTO.getRegistrationId().equals(teacher.getRegistrationId())) {

            boolean regIdExists = teacherRepository.findByRegistrationId(teacherDTO.getRegistrationId()).isPresent();
            if (regIdExists) {
                throw new BadRequestException("A teacher with this registration ID already exists.");
            }
            teacher.setRegistrationId(teacherDTO.getRegistrationId());
        }

        // ✅ Update college
        if (StringUtils.hasText(collegeName)) {
            College college = collegeRepository.findByName(collegeName)
                    .orElseThrow(() -> new BadRequestException("College not found"));
            teacher.setCollege(college);
        }

        // ✅ Update department
        if (StringUtils.hasText(departmentName)) {
            Department department = departmentRepository.findByName(departmentName)
                    .orElseThrow(() -> new BadRequestException("Department not found"));
            teacher.setDepartment(department);
        }

        // ✅ Update courses
        if (teacherDTO.getCourseNames() != null && !teacherDTO.getCourseNames().isEmpty()) {
            List<Course> newCourses = courseRepository.findByCourseNameIn(teacherDTO.getCourseNames());
            teacher.setCourses(newCourses);
        }

        teacherRepository.save(teacher);
    }




    @Transactional
    public void updateAdminByUsername(String username, AdminDTO adminDTO) {
        if (!StringUtils.hasText(username)) {
            throw new BadRequestException("Username can't be empty");
        }

        username = username.toLowerCase();

        String finalUsername = username;
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Admin not found with username: " + finalUsername));

        if (StringUtils.hasText(adminDTO.getUserName())) {
            admin.setUsername(adminDTO.getUserName().toLowerCase());
        }




        adminRepository.save(admin);
    }

    @Transactional(readOnly = true)
    public StudentDTO getStudentByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new BadRequestException("Username can't be empty");
        }

        username = username.toLowerCase();

        String finalUsername = username;
        Student student = studentRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Student not found with username: " + finalUsername));

        StudentDTO studentDTO = new StudentDTO();
        studentDTO.setUsername(student.getUsername());
        studentDTO.setName(student.getName());
        studentDTO.setRollNo(student.getRollNo());
        studentDTO.setSemester(student.getSemester());
        studentDTO.setDepartment(student.getDepartmentName());
        studentDTO.setRole(student.getRole());

        // Map enrolments
        if (student.getEnrolments() != null && !student.getEnrolments().isEmpty()) {
            List<EnrolmentInfoDTO> enrolmentInfos = student.getEnrolments().stream()
                    .map(enrolment -> {
                        EnrolmentInfoDTO infoDTO = new EnrolmentInfoDTO();
                        Course course = enrolment.getCourse();

                        infoDTO.setCourseName(course.getCourseName());
                        infoDTO.setCourseCode(course.getCourseCode());
                        infoDTO.setMidtermAnswerSheetUrl(enrolment.getMidtermAnswerSheetUrl());
                        infoDTO.setEndtermAnswerSheetUrl(enrolment.getEndtermAnswerSheetUrl());
                        infoDTO.setMidtermMarks(enrolment.getMidtermMarks());
                        infoDTO.setEndtermMarks(enrolment.getEndtermMarks());

                        // Fetch assigned teacher
                        if (course.getTeachers() != null && !course.getTeachers().isEmpty()) {
                            Teacher teacher = course.getTeachers().get(0); // Pick first teacher
                            infoDTO.setTeacherName(teacher.getName());
                        } else {
                            infoDTO.setTeacherName("Not Assigned");
                        }

                        return infoDTO;
                    })
                    .collect(Collectors.toList());

            studentDTO.setEnrolments(enrolmentInfos);
        }

        return studentDTO;
    }

    @Transactional(readOnly = true)
    public List<StudentDTO> getStudentsBySemesterAndCollegeAndDepartment(
            int semester, String collegeName, String departmentName) {

        if (!StringUtils.hasText(collegeName) || !StringUtils.hasText(departmentName)) {
            throw new BadRequestException("College name and department name must be provided");
        }

        collegeName = collegeName.toLowerCase();
        departmentName = departmentName.toLowerCase();
        Department department = departmentRepository.findByName(departmentName)
                .orElseThrow(() -> new RuntimeException("Department not found"));
        College college = collegeRepository.findByName(collegeName)
                .orElseThrow(() -> new RuntimeException("College not found"));

        List<Student> students = studentRepository.findBySemesterDepartmentAndCollege(
                semester, department, college
        );




        return students.stream().map(student -> {
            StudentDTO dto = new StudentDTO();
            dto.setUsername(student.getUsername());
            dto.setName(student.getName());
            dto.setRollNo(student.getRollNo());
            dto.setSemester(student.getSemester());
            dto.setDepartment(student.getDepartmentName());
            dto.setRole(student.getRole());

            if (student.getEnrolments() != null && !student.getEnrolments().isEmpty()) {
                List<EnrolmentInfoDTO> enrolmentInfos = student.getEnrolments().stream()
                        .map(enrolment -> {
                            EnrolmentInfoDTO infoDTO = new EnrolmentInfoDTO();
                            Course course = enrolment.getCourse();

                            infoDTO.setCourseName(course.getCourseName());
                            infoDTO.setCourseCode(course.getCourseCode());
                            infoDTO.setMidtermMarks(enrolment.getMidtermMarks());
                            infoDTO.setEndtermMarks(enrolment.getEndtermMarks());

                            if (course.getTeachers() != null && !course.getTeachers().isEmpty()) {
                                Teacher teacher = course.getTeachers().get(0);
                                infoDTO.setTeacherName(teacher.getName());
                            } else {
                                infoDTO.setTeacherName("Not Assigned");
                            }

                            return infoDTO;
                        })
                        .collect(Collectors.toList());

                dto.setEnrolments(enrolmentInfos);
            }

            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Fetch a single teacher by username, including their department names and course names.
     */
    @Transactional(readOnly = true)
    public TeacherDTO getTeacherByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new BadRequestException("Username must be provided");
        }

        username = username.toLowerCase();

        String finalUsername = username;
        Teacher teacher = teacherRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Teacher not found with username: " + finalUsername));

        // Optional: initialize courses if lazy
        teacher.getCourses().size();

        TeacherDTO dto = new TeacherDTO();
        dto.setUsername(teacher.getUsername());
        dto.setName(teacher.getName());
        dto.setRegistrationId(teacher.getRegistrationId());
        dto.setPassword(teacher.getPassword());
        dto.setRole(teacher.getRole());

        Department department = teacher.getDepartment();
        dto.setDepartmentName(department != null ? department.getName() : null);

        dto.setCourseNames(
                teacher.getCourses().stream()
                        .map(Course::getCourseName)
                        .collect(Collectors.toList())
        );

        return dto;
    }


    @Transactional(readOnly = true)
    public List<TeacherDTO> getTeachers(String collegeName, String departmentName) {
        if (!StringUtils.hasText(collegeName) || !StringUtils.hasText(departmentName)) {
            throw new BadRequestException("College name and department name must be provided");
        }

        collegeName = collegeName.toLowerCase();
        departmentName = departmentName.toLowerCase();

        Department department = departmentRepository.findByName(departmentName)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        College college = collegeRepository.findByName(collegeName)
                .orElseThrow(() -> new RuntimeException("College not found"));

        List<Teacher> teachers = teacherRepository.findByCollegeAndDepartment(college, department);

        return teachers.stream()
                .map(teacher -> {
                    TeacherDTO dto = new TeacherDTO();
                    dto.setUsername(teacher.getUsername());
                    dto.setName(teacher.getName());
                    dto.setRegistrationId(teacher.getRegistrationId());

                    Department dept = teacher.getDepartment();
                    dto.setDepartmentName(dept != null ? dept.getName() : null);

                    dto.setCourseNames(
                            teacher.getCourses().stream()
                                    .map(Course::getCourseName)
                                    .collect(Collectors.toList())
                    );

                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void unassignTeacherFromCourse(String teacherName, String courseName) {
        if (!StringUtils.hasText(teacherName) || !StringUtils.hasText(courseName)) {
            throw new BadRequestException("Teacher username and course name must be provided");
        }

        teacherName = teacherName.toLowerCase();
        courseName = courseName.toLowerCase();

        Teacher teacher = teacherRepository.findByUsername(teacherName)
                .orElseThrow(() -> new BadRequestException("Teacher not found"));

        Course course = courseRepository.findByCourseName(courseName)
                .orElseThrow(() -> new BadRequestException("Course not found"));

        Department teacherDept = teacher.getDepartment();
        if (teacherDept == null) {
            throw new BadRequestException("Teacher's department not found");
        }

        if (!course.getDepartment().equals(teacherDept)) {
            throw new BadRequestException("Course does not belong to the teacher's department");
        }

        course.getTeachers().remove(teacher);
        teacher.getCourses().remove(course);

        courseRepository.save(course);
        teacherRepository.save(teacher);
    }


    @Transactional
    public void assignTeacherToCourse(String teacherUsername, String courseName) {
        if (!StringUtils.hasText(teacherUsername) || !StringUtils.hasText(courseName)) {
            throw new BadRequestException("Teacher username and course name must be provided");
        }

        teacherUsername = teacherUsername.toLowerCase();
        courseName = courseName.toLowerCase();

        Teacher teacher = teacherRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new BadRequestException("Teacher not found"));

        Course course = courseRepository.findByCourseName(courseName)
                .orElseThrow(() -> new BadRequestException("Course not found"));

        Department teacherDept = teacher.getDepartment();
        if (teacherDept == null) {
            throw new BadRequestException("Teacher's department not found");
        }

        if (!course.getDepartment().equals(teacherDept)) {
            throw new BadRequestException("Course does not belong to the teacher's department");
        }

        if (!course.getTeachers().isEmpty()) {
            Teacher assignedTeacher = course.getTeachers().stream().findFirst().get();
            if (!assignedTeacher.getUsername().equals(teacherUsername)) {
                throw new BadRequestException("Course is already assigned to teacher: "
                        + assignedTeacher.getName() + " (" + assignedTeacher.getUsername() + ")");
            }
        }

        if (!teacher.getCourses().contains(course)) {
            teacher.getCourses().add(course);
        }

        if (!course.getTeachers().contains(teacher)) {
            course.getTeachers().add(teacher);
        }

        courseRepository.save(course);
        teacherRepository.save(teacher);
    }



    @Transactional
    @Retryable(
            value = { Exception.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000))
    public void addCollege(CollegeDTO collegeDTO) {
        if (!StringUtils.hasText(collegeDTO.getName())) {
            throw new BadRequestException("College name must be provided and not blank.");
        }

        College college = new College();
        college.setName(collegeDTO.getName().trim().toLowerCase());
        college.setEstablishedYear(collegeDTO.getEstablishedYear());
        college.setType(collegeDTO.getType());
        college.setLocation(collegeDTO.getLocation());

        collegeRepository.save(college);

        if (StringUtils.hasText(collegeDTO.getAdminUsername())) {
            String username = collegeDTO.getAdminUsername().toLowerCase();

            if (adminRepository.findByUsername(username).isPresent()) {
                throw new RuntimeException("Admin already exists with username: " + username);
            }

            Admin newAdmin = new Admin();
            newAdmin.setUsername(username);
            newAdmin.setCollege(college);
            newAdmin.setRole("ROLE_ADMIN");
            newAdmin.setName(collegeDTO.getAdminName());
            newAdmin.setPrimary(true);

            // Generate and encode random password
            String generatedPassword = generateRandomPassword();
            newAdmin.setPassword(new BCryptPasswordEncoder(10).encode(generatedPassword));

            adminRepository.save(newAdmin);

            // Send welcome email with credentials
            String subject = "Welcome to AutoEvaluator - Your Admin Account Created";
            String body = "Dear " + username + ",\n\n" +
                    "Welcome to AutoEvaluator! Your admin account has been created.\n\n" +
                    "Here are your login credentials:\n" +
                    "Username: " + username + "\n" +
                    "Password: " + generatedPassword + "\n\n" +
                    "Please change your password after logging in.\n\n" +
                    "Happy Evaluations!\n" +
                    "AutoEvaluator Team";

            emailService.sendSimpleEmail(username, subject, body);
        }
    }


    private String generateRandomPassword() {
        // Simple random password generator (can improve)
        return UUID.randomUUID().toString().substring(0, 8);
    }


    @Transactional
    public void addDepartmentToCollege(String collegeName, DepartmentDTO departmentDTO) {
        // Find the college by name
        College college = collegeRepository.findByName(collegeName)
                .orElseThrow(() -> new RuntimeException("College not found"));

        // Normalize department name: trim and convert to lowercase
        String normalizedDeptName = departmentDTO.getName().trim().toLowerCase();

        // Check if department already exists in this college (case-insensitive)
        boolean departmentExists = college.getDepartments().stream()
                .anyMatch(dept -> dept.getName().trim().toLowerCase().equals(normalizedDeptName));

        if (departmentExists) {
            throw new BadRequestException("Department with this name already exists in the college");
        }
        // Create new department and associate
        Department department = new Department();
        department.setName(normalizedDeptName); // save normalized name
        department.setCollege(college);
        // Save department
        departmentRepository.save(department);
    }

    // Update department inside a college
    @Transactional
    public void updateDepartmentInCollege(String collegeName, String departmentName, DepartmentDTO departmentDTO) {
        College college = collegeRepository.findByName(collegeName)
                .orElseThrow(() -> new RuntimeException("College not found"));

        Department department = college.getDepartments().stream()
                .filter(dep -> dep.getName().equalsIgnoreCase(departmentName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Department not found"));

        // Normalize new department name (trim + lowercase)
        String newNameNormalized = departmentDTO.getName().trim().toLowerCase();

        // Check for duplicate department name (exclude current department)
        boolean exists = college.getDepartments().stream()
                .filter(dep -> !dep.getId().equals(department.getId())) // exclude current
                .anyMatch(dep -> dep.getName().trim().toLowerCase().equals(newNameNormalized));

        if (exists) {
            throw new BadRequestException("Department name already exists in this college.");
        }

        // Set normalized name to department
        department.setName(newNameNormalized);

        // Save updated department
        departmentRepository.save(department);
    }


    // Delete department from college
    @Transactional
    public void deleteDepartmentFromCollege(String collegeName, String departmentName) {
        College college = collegeRepository.findByName(collegeName)
                .orElseThrow(() -> new RuntimeException("College not found"));

        // Normalize departmentName for consistent search
        String normalizedDeptName = departmentName.trim().toLowerCase();

        // Find department by normalized name and belonging to this college
        Department department = college.getDepartments().stream()
                .filter(dep -> dep.getName().trim().toLowerCase().equals(normalizedDeptName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Department not found in this college"));

        college.getDepartments().remove(department);
        collegeRepository.save(college);

        departmentRepository.delete(department);
    }


    // Fetch full College details including Departments and Courses
    public CollegeDTO getCollegeByName(String collegeName) {
        College college = collegeRepository.findByName(collegeName)
                .orElseThrow(() -> new RuntimeException("College not found with name: " + collegeName));

        // Map College entity to CollegeDTO
        CollegeDTO collegeDTO = new CollegeDTO();
        collegeDTO.setName(college.getName());

        List<DepartmentDTO> departmentDTOs = new ArrayList<>();
        for (Department department : college.getDepartments()) {
            DepartmentDTO departmentDTO = new DepartmentDTO();
            departmentDTO.setName(department.getName());

            List<CourseDTO> courseDTOs = new ArrayList<>();
            for (Course course : department.getCourses()) {
                CourseDTO courseDTO = new CourseDTO();
                courseDTO.setCourseName(course.getCourseName());
                courseDTO.setCourseCode(course.getCourseCode());
                courseDTO.setCourseCredits(course.getCourseCredits());
                courseDTO.setSemester(course.getSemester());
                courseDTOs.add(courseDTO);
            }

            departmentDTO.setCourses(courseDTOs);
            departmentDTOs.add(departmentDTO);
        }

        collegeDTO.setDepartments(departmentDTOs);
        return collegeDTO;
    }

    // Fetch a Department details inside a College
    public DepartmentDTO getDepartmentFromCollege(String collegeName, String departmentName) {
        College college = collegeRepository.findByName(collegeName)
                .orElseThrow(() -> new RuntimeException("College not found with name: " + collegeName));

        Department department = college.getDepartments().stream()
                .filter(dep -> dep.getName().equalsIgnoreCase(departmentName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Department not found in college: " + departmentName));

        DepartmentDTO departmentDTO = new DepartmentDTO();
        departmentDTO.setName(department.getName());

        List<CourseDTO> courseDTOs = new ArrayList<>();
        for (Course course : department.getCourses()) {
            CourseDTO courseDTO = new CourseDTO();
            courseDTO.setCourseName(course.getCourseName());
            courseDTO.setCourseCode(course.getCourseCode());
            courseDTO.setCourseCredits(course.getCourseCredits());
            courseDTO.setSemester(course.getSemester());
            courseDTOs.add(courseDTO);
        }

        departmentDTO.setCourses(courseDTOs);

        return departmentDTO;
    }


    // In CollegeService.java

    // Add a course
    @Transactional
    public void addCourseToDepartment(String collegeName, String departmentName, CourseDTO courseDTO) {
        College college = collegeRepository.findByName(collegeName)
                .orElseThrow(() -> new BadRequestException("College not found"));

        Department department = departmentRepository.findByNameAndCollege(departmentName, college)
                .orElseThrow(() -> new BadRequestException("Department not found in given college"));

        int semester = courseDTO.getSemester();
        if (semester < 1 || semester > 8) {
            throw new BadRequestException("Semester must be between 1 and 8.");
        }

        String normalizedCourseName = courseDTO.getCourseName().toLowerCase();

        boolean nameExistsInDepartment = courseRepository.existsByCourseNameIgnoreCaseAndDepartment(
                normalizedCourseName, department);
        if (nameExistsInDepartment) {
            throw new BadRequestException("Course name already exists in the department.");
        }

        boolean codeExistsInDepartment = courseRepository.existsByCourseCodeAndDepartment(
                courseDTO.getCourseCode(), department);
        if (codeExistsInDepartment) {
            throw new BadRequestException("Course code already exists in the department.");
        }

        // ✅ Save course
        Course course = new Course();
        course.setCourseName(normalizedCourseName);
        course.setCourseCode(courseDTO.getCourseCode());
        course.setCourseCredits(courseDTO.getCourseCredits());
        course.setSemester(semester);
        course.setDepartment(department);

        course = courseRepository.save(course); // IMPORTANT: get managed course with ID

        // ✅ Enroll existing students of same department & semester in this new course
        List<Student> students = studentRepository.findByDepartmentNameAndSemesterAndCollege(departmentName, semester, college);
        for (Student student : students) {
            Enrolment enrolment = new Enrolment();
            enrolment.setStudent(student);
            enrolment.setCourse(course);
            enrolmentRepository.save(enrolment);
        }
    }




    // Read a course
    public CourseDTO getCourseFromDepartment(String collegeName, String departmentName, String courseName) {
        College college = collegeRepository.findByName(collegeName)
                .orElseThrow(() -> new RuntimeException("College not found"));

        Department department = departmentRepository.findByNameAndCollege(departmentName, college)
                .orElseThrow(() -> new RuntimeException("Department not found in given college"));

        Course course = courseRepository.findByCourseNameAndDepartment(courseName, department)
                .orElseThrow(() -> new RuntimeException("Course not found in department"));

        return new CourseDTO(
                course.getCourseName(),
                course.getCourseCode(),
                course.getCourseCredits(),
                course.getSemester(),
                department.getName(),
                collegeName
        );
    }

    @Transactional
    public void updateCourseInDepartment(String collegeName, String departmentName, String courseName, CourseDTO updatedCourseDTO) {
        College college = collegeRepository.findByName(collegeName)
                .orElseThrow(() -> new BadRequestException("College not found"));

        Department department = departmentRepository.findByNameAndCollege(departmentName, college)
                .orElseThrow(() -> new BadRequestException("Department not found in given college"));

        Course course = courseRepository.findByCourseNameAndDepartment(courseName.toLowerCase(), department)
                .orElseThrow(() -> new BadRequestException("Course not found in department"));

        String normalizedNewName = updatedCourseDTO.getCourseName() != null
                ? updatedCourseDTO.getCourseName().toLowerCase()
                : course.getCourseName();

        String newCourseCode = updatedCourseDTO.getCourseCode() != null
                ? updatedCourseDTO.getCourseCode()
                : course.getCourseCode();

        int newSemester = updatedCourseDTO.getSemester() != 0
                ? updatedCourseDTO.getSemester()
                : course.getSemester();

        if (newSemester < 1 || newSemester > 8) {
            throw new BadRequestException("Semester must be between 1 and 8.");
        }

        // ❌ Prevent duplicate course name in the department (across any semester)
        if (!normalizedNewName.equals(course.getCourseName()) &&
                courseRepository.existsByCourseNameIgnoreCaseAndDepartmentAndCourseCodeNot(
                        normalizedNewName, department, course.getCourseCode())) {
            throw new BadRequestException("Course name already exists in the department.");
        }

        // ❌ Prevent duplicate course code in the department (across any semester)
        if (!newCourseCode.equals(course.getCourseCode()) &&
                courseRepository.existsByCourseCodeAndDepartmentAndCourseNameNot(
                        newCourseCode, department, course.getCourseName())) {
            throw new BadRequestException("Course code already exists in the department.");
        }

        // ✅ Perform updates
        course.setCourseName(normalizedNewName);
        course.setCourseCode(newCourseCode);
        course.setSemester(newSemester);

        if (updatedCourseDTO.getCourseCredits() != null) {
            course.setCourseCredits(updatedCourseDTO.getCourseCredits());
        }

        courseRepository.save(course);
    }

    @Transactional
    public void deleteCourseFromDepartment(String collegeName, String departmentName, String courseName) {
        College college = collegeRepository.findByName(collegeName)
                .orElseThrow(() -> new BadRequestException("College not found"));

        Department department = departmentRepository.findByNameAndCollege(departmentName, college)
                .orElseThrow(() -> new BadRequestException("Department not found in given college"));

        String normalizedCourseName = courseName.toLowerCase();

        Course course = courseRepository.findByCourseNameAndDepartment(normalizedCourseName, department)
                .orElseThrow(() -> new BadRequestException("Course not found in department"));

        // Check if any teachers are assigned
        if (course.getTeachers() != null && !course.getTeachers().isEmpty()) {
            throw new BadRequestException("Cannot delete course: one or more teachers are assigned.");
        }

        // ❗️Check if any enrolments exist
        boolean hasEnrollments = enrolmentRepository.existsByCourse(course);
        if (hasEnrollments) {
            throw new BadRequestException("Cannot delete course: students are enrolled in it.");
        }

        department.getCourses().remove(course);
        courseRepository.delete(course);
        departmentRepository.save(department);
    }

    public List<CollegeDTO> getAllColleges() {
        AppUser user = getCurrentUser();
        String role = user.getRole();

        if ("ROLE_SUPERADMIN".equals(role)) {
            // Return all colleges
            List<College> colleges = collegeRepository.findAll();
            return colleges.stream().map(college -> {
                CollegeDTO collegeDTO = new CollegeDTO();
                collegeDTO.setName(college.getName());
                collegeDTO.setEstablishedYear(college.getEstablishedYear());
                collegeDTO.setType(college.getType());
                collegeDTO.setLocation(college.getLocation());
                collegeDTO.setDepartments(Collections.emptyList()); // optional
                return collegeDTO;
            }).collect(Collectors.toList());
        } else if ("ROLE_ADMIN".equals(role)) {
            Admin admin = (Admin) user;
            College college = admin.getCollege(); // Assumes Admin has a `@ManyToOne College college` field

            if (college == null) {
                return Collections.emptyList();
            }

            CollegeDTO collegeDTO = new CollegeDTO();
            collegeDTO.setName(college.getName());
            collegeDTO.setEstablishedYear(college.getEstablishedYear());
            collegeDTO.setType(college.getType());
            collegeDTO.setLocation(college.getLocation());
            collegeDTO.setDepartments(Collections.emptyList());

            return List.of(collegeDTO);
        }

        return Collections.emptyList(); // fallback for other roles
    }


    @Transactional
    public void updateCollege(String collegeName, CollegeDTO collegeDTO) {
        AppUser user = getCurrentUser();
        String role = user.getRole();

        Optional<College> optionalCollege = collegeRepository.findByName(collegeName);
        if (optionalCollege.isEmpty()) {
            throw new RuntimeException("College not found with name: " + collegeName);
        }

        College college = optionalCollege.get();

        if ("ROLE_ADMIN".equals(role)) {
            // Cast and check associated college
            Admin admin = (Admin) user;
            College adminCollege = admin.getCollege();

            if (adminCollege == null || !adminCollege.getName().equalsIgnoreCase(collegeName)) {
                throw new RuntimeException("Admins can only update their own associated college.");
            }
        }

        if ("ROLE_SUPERADMIN".equals(role) || "ROLE_ADMIN".equals(role)) {
            String newCollegeName = collegeDTO.getName();

            // Check for name conflict if updating name
            if (newCollegeName != null && !newCollegeName.equalsIgnoreCase(collegeName)) {
                Optional<College> collegeWithNewName = collegeRepository.findByName(newCollegeName);
                if (collegeWithNewName.isPresent()) {
                    throw new BadRequestException("A college with the name '" + newCollegeName + "' already exists.");
                }
                college.setName(newCollegeName);
            }

            // Update fields
            if (collegeDTO.getEstablishedYear() != null) {
                college.setEstablishedYear(collegeDTO.getEstablishedYear());
            }
            if (collegeDTO.getType() != null) {
                college.setType(collegeDTO.getType());
            }
            if (collegeDTO.getLocation() != null) {
                college.setLocation(collegeDTO.getLocation());
            }

            collegeRepository.save(college);
        } else {
            throw new RuntimeException("Only Admins or SuperAdmins can update colleges.");
        }
    }


    @Transactional
    public boolean deleteCollege(String collegeName) {
        AppUser user = getCurrentUser();
        String role = user.getRole();

        Optional<College> collegeOptional = collegeRepository.findByName(collegeName);
        if (collegeOptional.isEmpty()) {
            return false;
        }

        College college = collegeOptional.get();

        if ("ROLE_SUPERADMIN".equals(role)) {
            collegeRepository.delete(college);
            return true;
        }

        if ("ROLE_ADMIN".equals(role)) {
            Admin admin = (Admin) user;
            College adminCollege = admin.getCollege();

            if (adminCollege != null && adminCollege.getName().equalsIgnoreCase(collegeName)) {
                collegeRepository.delete(college);
                return true;
            } else {
                throw new RuntimeException("Admins can only delete their own associated college.");
            }
        }

        throw new RuntimeException("Unauthorized to delete college.");
    }



    public List<DepartmentDTOGPT> getDepartmentsByCollege(String collegeName) {
        College college = collegeRepository.findByName(collegeName)
                .orElseThrow(() -> new RuntimeException("College not found with name: " + collegeName));


        // Call the custom repository method to fetch departments with total courses and total teachers
        return departmentRepository.findDepartmentsByCollegeName(collegeName);

    }

    public List<CourseDTO> getCoursesFromDepartment(String collegeName, String departmentName) {
        College college = collegeRepository.findByName(collegeName)
                .orElseThrow(() -> new RuntimeException("College not found"));

        Department department = college.getDepartments().stream()
                .filter(dep -> dep.getName().equalsIgnoreCase(departmentName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Department not found in this college"));

        return department.getCourses().stream()
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


    // The BCryptPasswordEncoder bean

    @Transactional
    public void updatePassword(String username, String rawPassword) {
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder(10);
        String encodedPassword = bCryptPasswordEncoder.encode(rawPassword);

        boolean userFound = false;

        // Try updating teacher
        Optional<Teacher> teacherOpt = teacherRepository.findByUsername(username);
        if (teacherOpt.isPresent()) {
            Teacher teacher = teacherOpt.get();
            teacher.setPassword(encodedPassword);
            teacherRepository.save(teacher);
            userFound = true;
        }

        // Try updating student
        Optional<Student> studentOpt = studentRepository.findByUsername(username);
        if (studentOpt.isPresent()) {
            Student student = studentOpt.get();
            student.setPassword(encodedPassword);
            studentRepository.save(student);
            userFound = true;
        }

        // Try updating admin
        Optional<Admin> adminOpt = adminRepository.findByUsername(username);
        if (adminOpt.isPresent()) {

            Admin admin = adminOpt.get();
            if (admin.isPrimary())
                throw new BadRequestException("Unexpected Error Occured, try forget password instead");
            admin.setPassword(encodedPassword);
            adminRepository.save(admin);
            userFound = true;

        }


        // If user not found in any repo, throw error
        if (!userFound) {
            throw new RuntimeException("User not found in any role");
        }
    }

    private AppUser getCurrentUser() {
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return appUserRepository.findByUsername(userPrincipal.getUsername());
    }

    public boolean existsByUsername(String username) {
        System.out.println(appUserRepository.findByUsername(username));
        return appUserRepository.findByUsername(username) != null;
        // return appUserRepository.findByUsername(username); // or adminRepository
    }


    @Transactional
    public void sendOneTimePassword(String username) {
        if (!existsByUsername(username)) {
            throw new BadRequestException("No user found with that email.");
        }

        String generatedPassword = generateRandomPassword();

        updatePassword(username, generatedPassword);

        String subject = "Your One-Time Password";
        String body = "Dear " + username + ",\n\nYour one-time password is: " + generatedPassword +
                "\n\nPlease change it after logging in.\n\nRegards,\nAdmin Team";

        emailService.sendSimpleEmail(username, subject, body);
    }
    public Object getAccountDetailsByRole() {
        AppUser user = getCurrentUser();

        return switch (user.getRole().toUpperCase()) {
            case "ROLE_STUDENT" -> studentRepository.findByUsername(user.getUsername())
                    .map(s -> {
                        StudentDTO dto = new StudentDTO();
                        dto.setUsername(s.getUsername());
                        dto.setName(s.getName());
                        dto.setRollNo(s.getRollNo());
                        dto.setSemester(s.getSemester());
                        dto.setCollege(s.getCollege().getName());
                        dto.setDepartment(s.getDepartment().getName());
                        dto.setRole("STUDENT");
                        dto.setPassword(null); // hide password
                        // leave enrolments and year empty if not needed
                        return dto;
                    })
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            case "ROLE_TEACHER" -> teacherRepository.findByUsername(user.getUsername())
                    .map(t -> {
                        TeacherDTO dto = new TeacherDTO();
                        dto.setUsername(t.getUsername());
                        dto.setName(t.getName());
                        dto.setRegistrationId(t.getRegistrationId());
                        dto.setDepartmentName(t.getDepartment() != null ? t.getDepartment().getName() : null);
                        dto.setCourseNames(t.getCourses().stream().map(c -> c.getCourseName()).toList());
                        dto.setRole("TEACHER");
                        dto.setPassword(null); // hide password
                        return dto;
                    })
                    .orElseThrow(() -> new RuntimeException("Teacher not found"));

            case "ROLE_ADMIN" -> adminRepository.findByUsername(user.getUsername())
                    .map(a -> new AdminDTO(
                            a.getId(),
                            a.getName(),
                            a.getUsername()
                    ))
                    .orElseThrow(() -> new RuntimeException("Admin not found"));

            case "ROLE_SUPERADMIN" -> superAdminRepository.findByUsername(user.getUsername())
                    .map(sa -> {
                        // Safe cast
                        return new SuperAdminDTO(
                                ((SuperAdmin) sa).getUsername(),
                                ((SuperAdmin) sa).getName(),
                                "SUPERADMIN"
                        );
                    })
                    .orElseThrow(() -> new RuntimeException("SuperAdmin not found"));

            default -> throw new RuntimeException("Unknown role: " + user.getRole());
        };
    }




    public void changePassword(ChangePasswordRequest request) {
        AppUser currentUser = getCurrentUser();
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

        if (!encoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            throw new BadRequestException("Please enter correct existing password");
        }
        if (request.getNewPassword().length() < 6) {
            throw new BadRequestException("Password must have at least 6 characters");
        }

        currentUser.setPassword(encoder.encode(request.getNewPassword()));
        appUserRepository.save(currentUser);
    }

    public List<StudentDTO> searchStudentByEmail(String email) {
        List<Student> students = studentRepository.findTop10ByUsernameContainingIgnoreCase(email);
        return students.stream().map(this::convertToDTO).toList();
    }

    private StudentDTO convertToDTO(Student student) {
        StudentDTO dto = new StudentDTO();
        dto.setUsername(student.getUsername());
        dto.setPassword(student.getPassword());
        dto.setRole(student.getRole());
        dto.setRollNo(student.getRollNo());
        dto.setName(student.getName());
        dto.setSemester(student.getSemester());
        if (student.getSemester() == 1 || student.getSemester() == 2)
            dto.setYear(1);
        if (student.getSemester() == 3 || student.getSemester() == 4)
            dto.setYear(2);
        if (student.getSemester() == 5 || student.getSemester() == 6)
            dto.setYear(3);
        if (student.getSemester() == 7 || student.getSemester() == 8)
            dto.setYear(4);
        dto.setDepartment(student.getDepartment() != null ? student.getDepartment().getName() : null);
        dto.setCollege(student.getCollege() != null ? student.getCollege().getName() : null);



        return dto;
    }

    public List<Teacher> searchTeacherByEmail(String email) {
        return teacherRepository.findTop10ByUsernameContainingIgnoreCase(email);
    }


    public List<AdminDTO> getAllAdmins() {
        Admin currentAdmin = (Admin) getCurrentUser(); // Cast to Admin
        College college = currentAdmin.getCollege();

        return adminRepository.findByCollege(college)
                .stream()
                .map(admin -> new AdminDTO(
                        admin.getId(),
                        admin.getName(),
                        admin.getUsername()
                ))
                .collect(Collectors.toList());
    }

}
