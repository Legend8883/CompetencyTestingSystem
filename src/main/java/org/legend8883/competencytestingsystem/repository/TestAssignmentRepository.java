package org.legend8883.competencytestingsystem.repository;

import org.legend8883.competencytestingsystem.entity.Test;
import org.legend8883.competencytestingsystem.entity.TestAssignment;
import org.legend8883.competencytestingsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TestAssignmentRepository extends JpaRepository<TestAssignment, Long> {

    List<TestAssignment> findByUser(User user);

    // Найти активные назначения пользователя (не выполненные, не просроченные)
    @Query("SELECT ta FROM TestAssignment ta " +
            "WHERE ta.user = :user " +
            "AND ta.isActive = true " +
            "AND ta.isCompleted = false " +
            "AND (ta.deadline IS NULL OR ta.deadline > :now)")
    List<TestAssignment> findActiveAssignmentsByUser(
            @Param("user") User user,
            @Param("now") LocalDateTime now);

    List<TestAssignment> findByTest(Test test);

    Optional<TestAssignment> findByUserAndTest(User user, Test test);

    // Найти просроченные назначения
    @Query("SELECT ta FROM TestAssignment ta " +
            "WHERE ta.isActive = true " +
            "AND ta.isCompleted = false " +
            "AND ta.deadline IS NOT NULL " +
            "AND ta.deadline <= :now")
    List<TestAssignment> findOverdueAssignments(@Param("now") LocalDateTime now);

    List<TestAssignment> findByAssignedBy(User assignedBy);
}
