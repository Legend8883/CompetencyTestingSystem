package org.legend8883.competencytestingsystem.service;

import lombok.RequiredArgsConstructor;
import org.legend8883.competencytestingsystem.dto.request.AssignTestRequest;
import org.legend8883.competencytestingsystem.entity.*;
import org.legend8883.competencytestingsystem.repository.TestAssignmentRepository;
import org.legend8883.competencytestingsystem.repository.TestRepository;
import org.legend8883.competencytestingsystem.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestAssignmentService {

    private final TestAssignmentRepository testAssignmentRepository;
    private final TestRepository testRepository;
    private final UserRepository userRepository;

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

            // Проверить не назначен ли уже тест
            if (testAssignmentRepository.findByUserAndTest(user, test).isPresent()) {
                continue; // Уже назначен, пропускаем
            }

            // Создать назначение
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

        // ИСПРАВЛЕНИЕ: Загружаем тесты с вопросами для корректного маппинга
        List<Test> tests = assignments.stream()
                .map(TestAssignment::getTest)
                .filter(Test::getIsActive)
                .map(test -> testRepository.findByIdWithQuestions(test.getId()).orElse(test)) // Загружаем полные данные
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

    public List<Test> getTestTests(Long employeeId) {
        System.out.println("=== DEBUG: getTestTests for employee " + employeeId + " ===");

        // Находим пользователя
        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // Находим всех HR
        List<User> hrs = userRepository.findByRole(Role.HR);

        if (hrs.isEmpty()) {
            System.out.println("WARNING: No HR found in system");
            return new ArrayList<>();
        }

        User hr = hrs.get(0); // Берем первого HR

        // Создаем тестовые данные
        List<Test> tests = new ArrayList<>();

        // Тест 1 - назначен пользователю
        Test test1 = new Test();
        test1.setId(1L);
        test1.setTitle("Тестовый тест по Java");
        test1.setDescription("Базовые вопросы по Java");
        test1.setTimeLimitMinutes(30);
        test1.setPassingScore(70);
        test1.setIsActive(true);
        test1.setCreatedBy(hr);
        test1.setCreatedAt(LocalDateTime.now());

        List<Question> questions1 = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Question q = new Question();
            q.setId((long) (i + 1));
            q.setText("Вопрос " + (i + 1) + " по Java");
            q.setMaxScore(10);
            q.setOrderIndex(i);
            q.setType(QuestionType.SINGLE_CHOICE);
            q.setTest(test1);
            questions1.add(q);
        }
        test1.setQuestions(questions1);

        tests.add(test1);

        // Тест 2 - назначен пользователю
        Test test2 = new Test();
        test2.setId(2L);
        test2.setTitle("Тест по SQL");
        test2.setDescription("Основы SQL запросов");
        test2.setTimeLimitMinutes(45);
        test2.setPassingScore(60);
        test2.setIsActive(true);
        test2.setCreatedBy(hr);
        test2.setCreatedAt(LocalDateTime.now());

        List<Question> questions2 = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            Question q = new Question();
            q.setId((long) (i + 11));
            q.setText("SQL вопрос " + (i + 1));
            q.setMaxScore(5);
            q.setOrderIndex(i);
            q.setType(QuestionType.SINGLE_CHOICE);
            q.setTest(test2);
            questions2.add(q);
        }
        test2.setQuestions(questions2);

        tests.add(test2);

        // Тест 3 - не назначен или не активен
        Test test3 = new Test();
        test3.setId(3L);
        test3.setTitle("Тест по Spring Framework");
        test3.setDescription("Основы Spring Boot");
        test3.setTimeLimitMinutes(60);
        test3.setPassingScore(75);
        test3.setIsActive(false);
        test3.setCreatedBy(hr);
        test3.setCreatedAt(LocalDateTime.now());

        tests.add(test3);

        System.out.println("Создано тестов: " + tests.size());

        // Создаем назначения для активных тестов
        for (Test test : tests) {
            if (test.getIsActive()) {
                TestAssignment assignment = new TestAssignment();
                assignment.setTest(test);
                assignment.setUser(employee);
                assignment.setAssignedBy(hr);
                assignment.setAssignedAt(LocalDateTime.now());
                assignment.setDeadline(LocalDateTime.now().plusDays(7));
                assignment.setIsActive(true);
                assignment.setIsCompleted(false);

                testAssignmentRepository.save(assignment);
                System.out.println("Created assignment for test: " + test.getTitle());
            }
        }

        System.out.println("=== END DEBUG ===");

        return tests.stream()
                .filter(Test::getIsActive)
                .collect(Collectors.toList());
    }


}
