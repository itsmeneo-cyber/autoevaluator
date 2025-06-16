package com.autoevaluator.domain.repositories;

import com.autoevaluator.domain.entity.Course;
import com.autoevaluator.domain.entity.Enrolment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrolmentRepository extends JpaRepository<Enrolment, Long> {
    List<Enrolment> findByStudentId(Long studentId);
    List<Enrolment> findByCourseId(Long courseId);
    boolean existsByCourse(Course course);
    // In EnrolmentRepository
    Optional<Enrolment> findByStudentIdAndCourseCourseName(Long studentId, String courseName);

    List<Enrolment> findByCourse(Course course); // <-- Add this line

}
