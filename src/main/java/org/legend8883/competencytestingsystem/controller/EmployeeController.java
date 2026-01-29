package org.legend8883.competencytestingsystem.controller;

import lombok.RequiredArgsConstructor;
import org.legend8883.competencytestingsystem.dto.request.StartTestRequest;
import org.legend8883.competencytestingsystem.dto.request.SubmitAnswerRequest;
import org.legend8883.competencytestingsystem.dto.response.ApiResponse;
import org.legend8883.competencytestingsystem.dto.response.TestProgressResponse;
import org.legend8883.competencytestingsystem.dto.response.TestResponse;
import org.legend8883.competencytestingsystem.entity.AnswerOption;
import org.legend8883.competencytestingsystem.entity.Question;
import org.legend8883.competencytestingsystem.entity.Test;
import org.legend8883.competencytestingsystem.entity.User;
import org.legend8883.competencytestingsystem.mapper.TestMapper;
import org.legend8883.competencytestingsystem.repository.QuestionRepository;
import org.legend8883.competencytestingsystem.repository.TestRepository;
import org.legend8883.competencytestingsystem.service.AttemptService;
import org.legend8883.competencytestingsystem.service.TestAssignmentService;
import org.legend8883.competencytestingsystem.service.TestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private final TestAssignmentService testAssignmentService;
    private final AttemptService attemptService;
    private final TestMapper testMapper;

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;

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
        // TODO: Реализовать получение попыток сотрудника
        return ResponseEntity.ok(ApiResponse.success("Мои попытки", List.of()));
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
}
