package com.autoevaluator.domain.repositories;

import com.autoevaluator.domain.entity.Course;
import com.autoevaluator.domain.entity.ReleaseStatus;
import com.autoevaluator.domain.entity.AnswerSheetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReleaseStatusRepository extends JpaRepository<ReleaseStatus, Long> {
    List<ReleaseStatus> findByCourse(Course course);
    Optional<ReleaseStatus> findByCourseAndAnswerSheetTypeAndAssignmentNumber(Course course, AnswerSheetType type, Integer assignmentNumber);
    Optional<ReleaseStatus> findByCourseAndAnswerSheetType(Course course, AnswerSheetType type);
}
