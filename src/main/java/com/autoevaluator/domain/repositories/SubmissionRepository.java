package com.autoevaluator.domain.repositories;

import com.autoevaluator.domain.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
//    List<Submission> findByEnrolmentStudentId(Long studentId);
//    List<Submission> findByEnrolmentCourseId(Long courseId);
}
