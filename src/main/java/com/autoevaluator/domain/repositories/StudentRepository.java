package com.autoevaluator.domain.repositories;

import com.autoevaluator.domain.entity.College;
import com.autoevaluator.domain.entity.Course;
import com.autoevaluator.domain.entity.Department;
import com.autoevaluator.domain.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByUsername(String username);

    @Query("SELECT s FROM Student s JOIN s.enrolments e JOIN e.course c WHERE s.departmentName = :departmentName AND s.semester = :semester AND c.courseName = :courseName")
    List<Student> findByDepartmentAndSemesterAndCourse(
            @Param("departmentName") String departmentName,
            @Param("semester") Integer semester,
            @Param("courseName") String courseName);

    List<Student> findBySemesterAndCollegeNameAndDepartmentName(int semester, String collegeName, String departmentName);

    @Query("SELECT s FROM Student s WHERE s.semester = :semester AND s.departmentName = :departmentName AND s.college.name = :collegeName")
    List<Student> findBySemesterDepartmentAndCollegeName(
            @Param("semester") int semester,
            @Param("departmentName") String departmentName,
            @Param("collegeName") String collegeName
    );


    boolean existsByUsername(String username);

    boolean existsByRollNo(String rollNo);

    List<Student> findByDepartmentNameAndSemesterAndCollege(String departmentName, int semester, College college);

    @Query("""
    SELECT s FROM Student s 
    JOIN s.enrolments e 
    WHERE s.department = :department 
    AND s.college = :college 
    AND s.semester = :semester 
    AND e.course = :course
""")
    List<Student> findByCollegeDepartmentSemesterAndCourse(
            @Param("college") College college,
            @Param("department") Department department,
            @Param("semester") int semester,
            @Param("course") Course course
    );


    @Query("SELECT s FROM Student s WHERE s.semester = :semester AND s.department = :department AND s.college = :college")
    List<Student> findBySemesterDepartmentAndCollege(
            @Param("semester") int semester,
            @Param("department") Department department,
            @Param("college") College college
    );

    List<Student> findTop10ByUsernameContainingIgnoreCase(String email);
    Optional<Student> findByUsernameAndCollegeAndDepartmentAndSemester(
            String username,
            College college,
            Department department,
            int semester
    );

}
