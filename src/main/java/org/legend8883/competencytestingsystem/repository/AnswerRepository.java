package org.legend8883.competencytestingsystem.repository;


import org.legend8883.competencytestingsystem.entity.Answer;
import org.legend8883.competencytestingsystem.entity.Attempt;
import org.legend8883.competencytestingsystem.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findByAttempt(Attempt attempt);

    Optional<Answer> findByAttemptAndQuestion(Attempt attempt, Question question);

    // Найти ответы попытки с вопросами
    @Query("SELECT DISTINCT a FROM Answer a " +
            "LEFT JOIN FETCH a.question q " +
            "LEFT JOIN FETCH q.options " +  // Добавляем загрузку вариантов вопроса
            "WHERE a.attempt = :attempt " +
            "ORDER BY q.orderIndex")
    List<Answer> findByAttemptWithQuestions(@Param("attempt") Attempt attempt);

    // Найти открытые вопросы требующие проверки
    @Query("SELECT a FROM Answer a " +
            "LEFT JOIN FETCH a.question q " +
            "LEFT JOIN FETCH a.attempt att " +
            "LEFT JOIN FETCH att.user " +
            "WHERE q.type = org.legend8883.competencytestingsystem.entity.QuestionType.OPEN_ANSWER " +
            "AND a.assignedScore IS NULL " +
            "AND att.status = org.legend8883.competencytestingsystem.entity.AttemptStatus.EVALUATING " +
            "ORDER BY a.answeredAt ASC")
    List<Answer> findOpenAnswersForEvaluation();

    long countByAttempt(Attempt attempt);

    @Query("SELECT a FROM Answer a LEFT JOIN FETCH a.question WHERE a.id = :id")
    Optional<Answer> findByIdWithQuestion(@Param("id") Long id);
}
