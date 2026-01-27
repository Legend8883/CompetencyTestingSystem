package org.legend8883.competencytestingsystem.repository;

import org.legend8883.competencytestingsystem.entity.Attempt;
import org.legend8883.competencytestingsystem.entity.AttemptStatus;
import org.legend8883.competencytestingsystem.entity.Test;
import org.legend8883.competencytestingsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttemptRepository extends JpaRepository<Attempt, Long> {

    List<Attempt> findByUser(User user);

    Optional<Attempt> findByUserAndTest(User user, Test test);

    List<Attempt> findByUserAndStatus(User user, AttemptStatus status);

    List<Attempt> findByTest(Test test);

    // Найти попытки требующие автоматической отправки (таймер истек)
    @Query("SELECT a FROM Attempt a WHERE a.status = org.legend8883.competencytestingsystem.entity.AttemptStatus.IN_PROGRESS " +
            "AND a.autoSubmitAt <= :now")
    List<Attempt> findAttemptsForAutoSubmit(@Param("now") LocalDateTime now);

    // Найти попытки для проверки HR (с открытыми вопросами)
    @Query("SELECT DISTINCT a FROM Attempt a " +
            "LEFT JOIN FETCH a.user " +
            "LEFT JOIN FETCH a.test " +
            "WHERE a.status = :status " +
            "ORDER BY a.completedAt DESC")
    List<Attempt> findByStatusWithDetails(@Param("status") AttemptStatus status);

    // Найти попытки по статусу
    List<Attempt> findByStatus(AttemptStatus status);
}
