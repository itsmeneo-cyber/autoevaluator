package com.autoevaluator.domain.repositories;

import com.autoevaluator.domain.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
//    List<Evaluation> findByEnrolmentStudentId(Long studentId);
//    List<Evaluation> findByEnrolmentCourseId(Long courseId);
}
