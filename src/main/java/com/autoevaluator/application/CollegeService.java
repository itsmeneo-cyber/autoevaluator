package com.autoevaluator.application;

import com.autoevaluator.domain.dto.CollegeDTO;
import com.autoevaluator.domain.dto.DepartmentDTO;
import com.autoevaluator.domain.dto.CourseDTO;
import com.autoevaluator.domain.dto.DepartmentDTOGPT;
import com.autoevaluator.domain.entity.College;
import com.autoevaluator.domain.entity.Department;
import com.autoevaluator.domain.entity.Course;
import com.autoevaluator.domain.entity.Teacher;
import com.autoevaluator.domain.exception.BadRequestException;
import com.autoevaluator.domain.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollegeService {

    private final CollegeRepository collegeRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;
    private final EnrolmentRepository enrolmentRepository;


    @Transactional
    public void addCollege(CollegeDTO collegeDTO) {
        if (!StringUtils.hasText(collegeDTO.getName())) {
            throw new BadRequestException("College name must be provided and not blank.");
        }

        College college = new College();
        college.setName(collegeDTO.getName().trim().toLowerCase());
        college.setEstablishedYear(collegeDTO.getEstablishedYear());
        college.setType(collegeDTO.getType());
        college.setLocation(collegeDTO.getLocation());

        // Assuming departments and courses are no longer passed, so ignoring those

        collegeRepository.save(college);
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

        // Normalize course name
        String normalizedCourseName = courseDTO.getCourseName().toLowerCase();

        // ❌ Ensure course name is not repeated in the department (any semester)
        boolean nameExistsInDepartment = courseRepository.existsByCourseNameIgnoreCaseAndDepartment(
                normalizedCourseName, department);
        if (nameExistsInDepartment) {
            throw new BadRequestException("Course name already exists in the department.");
        }

        // ❌ Ensure course code is not repeated in the department (any semester)
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

        courseRepository.save(course);
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
                departmentName,
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
        List<College> colleges = collegeRepository.findAll();
        System.out.println(colleges);
        // Manually map the College entity to CollegeDTO
        return colleges.stream().map(college -> {
            CollegeDTO collegeDTO = new CollegeDTO();
            collegeDTO.setName(college.getName());
            collegeDTO.setEstablishedYear(college.getEstablishedYear());
            collegeDTO.setType(college.getType());
            collegeDTO.setLocation(college.getLocation());

            // Leaving departments empty for now
            collegeDTO.setDepartments(Collections.emptyList());

            return collegeDTO;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void updateCollege(String collegeName, CollegeDTO collegeDTO) {
        Optional<College> optionalCollege = collegeRepository.findByName(collegeName);

        if (optionalCollege.isPresent()) {
            College college = optionalCollege.get();

            String newCollegeName = collegeDTO.getName();

            // Check if new college name is different and already exists for another college
            if (newCollegeName != null && !newCollegeName.equalsIgnoreCase(collegeName)) {
                Optional<College> collegeWithNewName = collegeRepository.findByName(newCollegeName);
                if (collegeWithNewName.isPresent()) {
                    throw new BadRequestException("A college with the name '" + newCollegeName + "' already exists.");
                }
            }

            // Update the college entity with the new values from the DTO
            if (newCollegeName != null) {
                college.setName(newCollegeName);
            }
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
            throw new RuntimeException("College not found with name: " + collegeName);
        }
    }

    public boolean deleteCollege(String collegeName) {
        Optional<College> collegeOptional = collegeRepository.findByName(collegeName);
        if (collegeOptional.isPresent()) {
            collegeRepository.delete(collegeOptional.get());
            return true;
        } else {
            return false;
        }
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



}
