package com.autoevaluator.domain.repositories;



import com.autoevaluator.domain.entity.Course;
import com.autoevaluator.domain.entity.Question;
import com.autoevaluator.domain.entity.QuestionPaper;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {


}

