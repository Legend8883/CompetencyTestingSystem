package org.legend8883.competencytestingsystem.repository;

import org.legend8883.competencytestingsystem.entity.Question;
import org.legend8883.competencytestingsystem.entity.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByTest(Test test);

    // Найти вопросы теста с вариантами ответов
    @Query("SELECT q FROM Question q LEFT JOIN FETCH q.options WHERE q.test = :test ORDER BY q.orderIndex")
    List<Question> findByTestWithOptions(@Param("test") Test test);

    // Найти вопрос с вариантами ответов по ID
    @Query("SELECT q FROM Question q LEFT JOIN FETCH q.options WHERE q.id = :id")
    Optional<Question> findByIdWithOptions(@Param("id") Long id);

    long countByTest(Test test);

    // Найти следующий порядковый номер для нового вопроса
    @Query("SELECT COALESCE(MAX(q.orderIndex), -1) + 1 FROM Question q WHERE q.test = :test")
    Integer findNextOrderIndex(@Param("test") Test test);
}
