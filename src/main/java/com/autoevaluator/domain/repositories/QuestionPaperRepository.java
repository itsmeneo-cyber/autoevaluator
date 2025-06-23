package com.autoevaluator.domain.repositories;



import com.autoevaluator.domain.entity.AnswerSheetType;
import com.autoevaluator.domain.entity.AppUser;
import com.autoevaluator.domain.entity.Course;
import com.autoevaluator.domain.entity.QuestionPaper;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionPaperRepository extends JpaRepository<QuestionPaper, Long> {
    List<QuestionPaper> findByCreatedBy(AppUser user);

    List<QuestionPaper> findByCourse(Course course);
    Optional<QuestionPaper> findByCourse_CourseNameAndIsMidtermTrue(String courseName);
    Optional<QuestionPaper> findByCourse_CourseNameAndIsEndtermTrue(String courseName);
    Optional<QuestionPaper> findByCourse_CourseNameAndIsAssignmentTrueAndAssignmentNumber(String courseName, int assignmentNumber);



}

