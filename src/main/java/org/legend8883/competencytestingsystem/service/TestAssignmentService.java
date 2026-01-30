package org.legend8883.competencytestingsystem.service;

import lombok.RequiredArgsConstructor;
import org.legend8883.competencytestingsystem.dto.request.AssignTestRequest;
import org.legend8883.competencytestingsystem.entity.*;
import org.legend8883.competencytestingsystem.repository.AttemptRepository;
import org.legend8883.competencytestingsystem.repository.TestAssignmentRepository;
import org.legend8883.competencytestingsystem.repository.TestRepository;
import org.legend8883.competencytestingsystem.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TestAssignmentService {

    private final TestAssignmentRepository testAssignmentRepository;
    private final TestRepository testRepository;
    private final UserRepository userRepository;
    private final AttemptRepository attemptRepository;

    // Назначить тест сотрудникам (HR функция)
    @Transactional
    public void assignTestToUsers(Long testId, AssignTestRequest request, Long hrId) {
        // 1. Найти тест
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found"));

        // 2. Найти HR
        User hr = userRepository.findById(hrId)
                .orElseThrow(() -> new RuntimeException("HR not found"));

        // 3. Проверить что тест принадлежит этому HR
        if (!test.getCreatedBy().getId().equals(hrId)) {
            throw new RuntimeException("You can only assign your own tests");
        }

        // 4. Назначить тест каждому сотруднику
        for (Long userId : request.getUserIds()) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

            // Проверить что пользователь - сотрудник
            if (user.getRole() != Role.EMPLOYEE) {
                throw new RuntimeException("User " + user.getEmail() + " is not an employee");
            }

            // ПРОВЕРКА: не назначен ли уже тест и не начал ли сотрудник его проходить
            Optional<TestAssignment> existingAssignment = testAssignmentRepository
                    .findByUserAndTest(user, test);

            if (existingAssignment.isPresent()) {
                // Проверяем, не начал ли уже сотрудник тест
                Optional<Attempt> existingAttempt = attemptRepository
                        .findByUserAndTest(user, test);

                if (existingAttempt.isPresent()) {
                    throw new RuntimeException("Сотрудник " + user.getEmail() +
                            " уже начал проходить этот тест");
                }

                // Если назначение существует, но тест не начат, обновляем его
                TestAssignment assignment = existingAssignment.get();
                assignment.setDeadline(request.getDeadline());
                assignment.setIsActive(true);
                testAssignmentRepository.save(assignment);
                continue;
            }

            // Создать новое назначение
            TestAssignment assignment = new TestAssignment();
            assignment.setTest(test);
            assignment.setUser(user);
            assignment.setAssignedBy(hr);
            assignment.setDeadline(request.getDeadline());
            assignment.setIsActive(true);
            assignment.setIsCompleted(false);

            testAssignmentRepository.save(assignment);
        }
    }

    // Получить доступные тесты для сотрудника
    public List<Test> getAvailableTestsForEmployee(Long employeeId) {
        System.out.println("=== DEBUG: getAvailableTestsForEmployee ===");
        System.out.println("Employee ID: " + employeeId);

        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        System.out.println("Employee found: " + employee.getEmail() + ", Role: " + employee.getRole());

        LocalDateTime now = LocalDateTime.now();
        System.out.println("Current time: " + now);

        List<TestAssignment> assignments = testAssignmentRepository
                .findActiveAssignmentsByUser(employee, now);

        System.out.println("Total assignments found: " + assignments.size());

        for (TestAssignment assignment : assignments) {
            System.out.println("Assignment ID: " + assignment.getId());
            System.out.println("  Test ID: " + assignment.getTest().getId());
            System.out.println("  Test Title: " + assignment.getTest().getTitle());
            System.out.println("  Test isActive: " + assignment.getTest().getIsActive());
            System.out.println("  Assignment isActive: " + assignment.getIsActive());
            System.out.println("  Assignment deadline: " + assignment.getDeadline());
            System.out.println("  Assignment isCompleted: " + assignment.getIsCompleted());
        }

        List<Test> tests = assignments.stream()
                .map(TestAssignment::getTest)
                .filter(Test::getIsActive)
                .toList();

        System.out.println("Available tests after filtering: " + tests.size());
        System.out.println("=== END DEBUG ===");

        return tests;
    }

    // Получить назначения теста (HR функция)
    public List<TestAssignment> getTestAssignments(Long testId, Long hrId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found"));

        // Проверить права
        if (!test.getCreatedBy().getId().equals(hrId)) {
            throw new RuntimeException("You can only view assignments for your own tests");
        }

        return testAssignmentRepository.findByTest(test);
    }


}
