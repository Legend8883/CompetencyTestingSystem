package org.legend8883.competencytestingsystem.repository;

import org.legend8883.competencytestingsystem.entity.AnswerOption;
import org.legend8883.competencytestingsystem.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnswerOptionRepository extends JpaRepository<AnswerOption, Long> {

    List<AnswerOption> findByQuestion(Question question);

    List<AnswerOption> findByQuestionAndIsCorrectTrue(Question question);

    Optional<AnswerOption> findByIdAndQuestion(Long id, Question question);

    void deleteByQuestion(Question question);

    long countByQuestionAndIsCorrectTrue(Question question);
}
