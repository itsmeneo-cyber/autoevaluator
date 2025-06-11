package com.autoevaluator.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class Question {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "question_seq_gen"
    )
    @SequenceGenerator(
            name = "question_seq_gen",
            sequenceName = "Question_SEQ",
            allocationSize = 1
    )
    private Long id;

    private String text;//this is question text like waht is the question

    private Double marks;
    @Column(columnDefinition = "TEXT")
    private String questionNumber;
    private String instructions;

    @ManyToOne
    private QuestionPaper questionPaper;

    @OneToOne(mappedBy = "question", cascade = CascadeType.ALL)
    private AnswerKey answerKey;

}