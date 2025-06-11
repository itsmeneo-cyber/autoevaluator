package com.autoevaluator.domain.repositories;

import com.autoevaluator.domain.entity.Course;
import com.autoevaluator.domain.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {



    // Change 'findByName' to 'findByCourseName' to match the property in the Course entity
    Optional<Course> findByCourseName(String courseName);

    Optional<Course> findByCourseCode(String courseCode);

    // The query to find courses by department name and semester looks fine
    @Query("SELECT c FROM Course c WHERE c.department.name = :departmentName AND c.semester = :semester")
    List<Course> findByDepartmentAndSemester(String departmentName, int semester);

    List<Course> findByCourseNameIn(List<String> courseNames);
    Optional<Course> findByCourseNameAndDepartment(String courseName, Department department);

    boolean existsByCourseNameAndDepartment(String courseName, Department department);
    boolean existsByCourseCodeAndDepartment(String courseCode, Department department);

    boolean existsByCourseNameAndDepartmentAndCourseCodeNot(String courseName, Department department, String courseCode);

    //boolean existsByCourseCodeAndDepartmentAndCourseNameNot(String courseCode, Department department, String courseName);
    boolean existsByCourseNameIgnoreCaseAndDepartmentAndSemester(String courseName, Department department, int semester);

    boolean existsByCourseCodeAndDepartmentAndSemester(String courseCode, Department department, int semester);

    boolean existsByCourseNameIgnoreCaseAndDepartment( String courseName, Department department );
    boolean existsByCourseNameIgnoreCaseAndDepartmentAndCourseCodeNot(String courseName, Department department, String courseCode);
    boolean existsByCourseCodeAndDepartmentAndCourseNameNot(String courseCode, Department department, String courseName);


}
