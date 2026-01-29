package org.legend8883.competencytestingsystem.controller;

import lombok.RequiredArgsConstructor;
import org.legend8883.competencytestingsystem.dto.request.StartTestRequest;
import org.legend8883.competencytestingsystem.dto.request.SubmitAnswerRequest;
import org.legend8883.competencytestingsystem.dto.response.ApiResponse;
import org.legend8883.competencytestingsystem.dto.response.TestProgressResponse;
import org.legend8883.competencytestingsystem.dto.response.TestResponse;
import org.legend8883.competencytestingsystem.dto.response.TestResultResponse;
import org.legend8883.competencytestingsystem.entity.*;
import org.legend8883.competencytestingsystem.mapper.TestMapper;
import org.legend8883.competencytestingsystem.repository.AttemptRepository;
import org.legend8883.competencytestingsystem.repository.QuestionRepository;
import org.legend8883.competencytestingsystem.repository.TestAssignmentRepository;
import org.legend8883.competencytestingsystem.repository.TestRepository;
import org.legend8883.competencytestingsystem.service.AttemptService;
import org.legend8883.competencytestingsystem.service.TestAssignmentService;
import org.legend8883.competencytestingsystem.service.TestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private final TestAssignmentService testAssignmentService;
    private final AttemptService attemptService;
    private final TestMapper testMapper;

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final TestAssignmentRepository testAssignmentRepository;
    private final AttemptRepository attemptRepository;

    // ДОСТУПНЫЕ ТЕСТЫ
    @GetMapping("/tests/available")
    public ResponseEntity<ApiResponse<List<TestResponse>>> getAvailableTests(
            @AuthenticationPrincipal User employee) {
        var tests = testAssignmentService.getAvailableTestsForEmployee(employee.getId());
        List<TestResponse> testResponses = tests.stream()
                .map(testMapper::toDto)
                .toList();

        return ResponseEntity.ok(ApiResponse.success("Доступные тесты", testResponses));
    }

    // ПРОХОЖДЕНИЕ ТЕСТА

    @PostMapping("/tests/start")
    public ResponseEntity<ApiResponse<TestProgressResponse>> startTest(
            @RequestBody StartTestRequest request,
            @AuthenticationPrincipal User employee) {
        TestProgressResponse response = attemptService.startTest(request, employee.getId());
        return ResponseEntity.ok(ApiResponse.success("Тест начат", response));
    }

    @GetMapping("/attempts/{attemptId}/progress")
    public ResponseEntity<ApiResponse<TestProgressResponse>> getTestProgress(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal User employee) {
        TestProgressResponse response = attemptService.getTestProgress(attemptId, employee.getId());
        return ResponseEntity.ok(ApiResponse.success("Прогресс теста", response));
    }

    @PostMapping("/attempts/{attemptId}/answers")
    public ResponseEntity<ApiResponse<TestProgressResponse>> submitAnswer(
            @PathVariable Long attemptId,
            @RequestBody SubmitAnswerRequest request,
            @AuthenticationPrincipal User employee) {
        TestProgressResponse response = attemptService.submitAnswer(attemptId, request, employee.getId());
        return ResponseEntity.ok(ApiResponse.success("Ответ сохранен", response));
    }

    @PostMapping("/attempts/{attemptId}/complete")
    public ResponseEntity<ApiResponse<TestProgressResponse>> completeTest(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal User employee) {
        TestProgressResponse response = attemptService.completeTest(attemptId, employee.getId());
        return ResponseEntity.ok(ApiResponse.success("Тест завершен", response));
    }

    // РЕЗУЛЬТАТЫ

    @GetMapping("/attempts")
    public ResponseEntity<ApiResponse<List<TestProgressResponse>>> getMyAttempts(
            @AuthenticationPrincipal User employee) {
        List<TestProgressResponse> attempts = attemptService.getMyAttempts(employee.getId());
        return ResponseEntity.ok(ApiResponse.success("Мои попытки", attempts));
    }

    @GetMapping("/test/{testId}/simple")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTestSimple(
            @PathVariable Long testId,
            @AuthenticationPrincipal User employee) {

        Map<String, Object> response = new HashMap<>();

        // 1. Проверяем тест
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found"));
        response.put("testId", test.getId());
        response.put("testTitle", test.getTitle());

        // 2. Получаем вопросы
        List<Question> questions = questionRepository.findByTestWithOptions(test);
        response.put("questionsCount", questions.size());

        // 3. Формируем первый вопрос
        if (!questions.isEmpty()) {
            Question question = questions.get(0);
            Map<String, Object> questionData = new HashMap<>();
            questionData.put("id", question.getId());
            questionData.put("text", question.getText());
            questionData.put("type", question.getType().name());
            questionData.put("orderIndex", question.getOrderIndex());

            // Варианты ответов
            if (question.getOptions() != null && !question.getOptions().isEmpty()) {
                List<Map<String, Object>> options = new ArrayList<>();
                for (AnswerOption option : question.getOptions()) {
                    Map<String, Object> optionData = new HashMap<>();
                    optionData.put("id", option.getId());
                    optionData.put("text", option.getText());
                    optionData.put("orderIndex", option.getOrderIndex());
                    options.add(optionData);
                }
                questionData.put("options", options);
                questionData.put("optionsCount", options.size());
            } else {
                questionData.put("optionsCount", 0);
            }

            response.put("currentQuestion", questionData);
        }

        return ResponseEntity.ok(ApiResponse.success("Test data", response));
    }

    @GetMapping("/attempts/{attemptId}/questions/{questionId}")
    public ResponseEntity<ApiResponse<TestProgressResponse>> getQuestion(
            @PathVariable Long attemptId,
            @PathVariable Long questionId,
            @AuthenticationPrincipal User employee) {
        TestProgressResponse response = attemptService.getQuestion(attemptId, questionId, employee.getId());
        return ResponseEntity.ok(ApiResponse.success("Вопрос получен", response));
    }

    @PostMapping("/attempts/{attemptId}/go-to-question")
    public ResponseEntity<ApiResponse<TestProgressResponse>> goToQuestion(
            @PathVariable Long attemptId,
            @RequestBody Map<String, Long> request,
            @AuthenticationPrincipal User employee) {
        Long questionId = request.get("questionId");
        TestProgressResponse response = attemptService.goToQuestion(attemptId, questionId, employee.getId());
        return ResponseEntity.ok(ApiResponse.success("Перешли к вопросу", response));
    }

    @GetMapping("/attempts/{attemptId}/results")
    public ResponseEntity<ApiResponse<TestResultResponse>> getTestResults(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal User employee) {

        // TODO: Реализовать метод получения детальных результатов
        // Должен возвращать:
        // 1. Все вопросы теста
        // 2. Ответы сотрудника
        // 3. Правильные ответы (для вопросов с выбором)
        // 4. Оценки HR (для открытых вопросов)
        // 5. Итоговый балл

        return ResponseEntity.ok(ApiResponse.success("Результаты теста", null));
    }

    @GetMapping("/tests/{testId}/details")
    public ResponseEntity<ApiResponse<TestResponse>> getTestDetails(
            @PathVariable Long testId,
            @AuthenticationPrincipal User employee) {

        // Получить тест
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found"));

        // Проверить назначение
        Optional<TestAssignment> assignment = testAssignmentRepository
                .findByUserAndTest(employee, test);

        if (assignment.isEmpty()) {
            throw new RuntimeException("Test is not assigned to you");
        }

        TestResponse response = testMapper.toDto(test);
        return ResponseEntity.ok(ApiResponse.success("Детали теста", response));
    }

    @GetMapping("/results/{attemptId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAttemptResults(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal User employee) {

        // Найти попытку
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        // Проверить права
        if (!attempt.getUser().getId().equals(employee.getId())) {
            throw new RuntimeException("This attempt belongs to another user");
        }

        // Создать базовый ответ
        Map<String, Object> response = new HashMap<>();
        response.put("attemptId", attempt.getId());
        response.put("testId", attempt.getTest().getId());
        response.put("testTitle", attempt.getTest().getTitle());
        response.put("score", attempt.getScore());
        response.put("status", attempt.getStatus().name());
        response.put("passed", attempt.getScore() >= attempt.getTest().getPassingScore());
        response.put("startedAt", attempt.getStartedAt());
        response.put("completedAt", attempt.getCompletedAt());

        // TODO: Добавить детали вопросов и ответов

        return ResponseEntity.ok(ApiResponse.success("Результаты попытки", response));
    }

    @GetMapping("/tests/test")
    public ResponseEntity<ApiResponse<List<TestResponse>>> getTestTests(
            @AuthenticationPrincipal User employee) {

        // Используем метод с employeeId
        var tests = testAssignmentService.getTestTests(employee.getId());
        List<TestResponse> testResponses = tests.stream()
                .map(testMapper::toDto)
                .toList();

        return ResponseEntity.ok(ApiResponse.success("Тестовые данные", testResponses));
    }
}
