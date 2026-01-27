package org.legend8883.competencytestingsystem.repository;

import org.legend8883.competencytestingsystem.entity.Test;
import org.legend8883.competencytestingsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestRepository extends JpaRepository<Test, Long> {

    List<Test> findByCreatedBy(User createdBy);

    List<Test> findByIsActiveTrue();

    List<Test> findByTitleContainingIgnoreCase(String title);

    // Найти тесты с вопросами
    @Query("SELECT DISTINCT t FROM Test t LEFT JOIN FETCH t.questions WHERE t.id = :id")
    Optional<Test> findByIdWithQuestions(@Param("id") Long id);

    List<Test> findByCreatedByAndIsActiveTrue(User createdBy);
}
