package org.legend8883.competencytestingsystem.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.legend8883.competencytestingsystem.dto.request.AssignTestRequest;
import org.legend8883.competencytestingsystem.dto.request.CreateTestRequest;
import org.legend8883.competencytestingsystem.dto.request.EvaluateAnswerRequest;
import org.legend8883.competencytestingsystem.dto.response.*;
import org.legend8883.competencytestingsystem.entity.*;
import org.legend8883.competencytestingsystem.mapper.AnswerMapper;
import org.legend8883.competencytestingsystem.mapper.AttemptMapper;
import org.legend8883.competencytestingsystem.repository.AttemptRepository;
import org.legend8883.competencytestingsystem.service.EvaluationService;
import org.legend8883.competencytestingsystem.service.TestAssignmentService;
import org.legend8883.competencytestingsystem.service.TestService;
import org.legend8883.competencytestingsystem.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hr")
@RequiredArgsConstructor
public class HrController {

    private final TestService testService;
    private final UserService userService;
    private final TestAssignmentService testAssignmentService;
    private final EvaluationService evaluationService;
    private final AnswerMapper answerMapper;
    private final AttemptMapper attemptMapper;

    private final AttemptRepository attemptRepository;

    // ========== ТЕСТЫ ==========

    @PostMapping("/tests")
    public ResponseEntity<ApiResponse<TestResponse>> createTest(
            @Valid @RequestBody CreateTestRequest request,
            @AuthenticationPrincipal User hr) {
        TestResponse response = testService.createTest(request, hr.getId());
        return ResponseEntity.ok(ApiResponse.success("Тест создан", response));
    }

    @GetMapping("/tests")
    public ResponseEntity<ApiResponse<List<TestResponse>>> getMyTests(
            @AuthenticationPrincipal User hr) {
        List<TestResponse> tests = testService.getTestsByHr(hr.getId());
        return ResponseEntity.ok(ApiResponse.success("Тесты получены", tests));
    }

    @GetMapping("/tests/{testId}")
    public ResponseEntity<ApiResponse<TestResponse>> getTest(
            @PathVariable Long testId,
            @AuthenticationPrincipal User hr) {
        TestResponse test = testService.getTestById(testId);
        return ResponseEntity.ok(ApiResponse.success("Тест получен", test));
    }

    @PatchMapping("/tests/{testId}/activate")
    public ResponseEntity<ApiResponse<TestResponse>> activateTest(
            @PathVariable Long testId,
            @AuthenticationPrincipal User hr) {
        TestResponse test = testService.toggleTestStatus(testId, hr.getId(), true);
        return ResponseEntity.ok(ApiResponse.success("Тест активирован", test));
    }

    @PatchMapping("/tests/{testId}/deactivate")
    public ResponseEntity<ApiResponse<TestResponse>> deactivateTest(
            @PathVariable Long testId,
            @AuthenticationPrincipal User hr) {
        TestResponse test = testService.toggleTestStatus(testId, hr.getId(), false);
        return ResponseEntity.ok(ApiResponse.success("Тест деактивирован", test));
    }

    // ========== ПОЛЬЗОВАТЕЛИ ==========

    @GetMapping("/employees")
    public ResponseEntity<ApiResponse<List<UserSimpleResponse>>> getAllEmployees() {
        List<UserSimpleResponse> employees = userService.getAllEmployees();
        return ResponseEntity.ok(ApiResponse.success("Сотрудники получены", employees));
    }

    @GetMapping("/employees/search")
    public ResponseEntity<ApiResponse<List<UserSimpleResponse>>> searchEmployees(
            @RequestParam String query) {
        List<UserSimpleResponse> employees = userService.searchUsers(query);
        return ResponseEntity.ok(ApiResponse.success("Результаты поиска", employees));
    }

    // ========== НАЗНАЧЕНИЕ ТЕСТОВ ==========

    @PostMapping("/tests/{testId}/assign")
    public ResponseEntity<ApiResponse<Void>> assignTest(
            @PathVariable Long testId,
            @Valid @RequestBody AssignTestRequest request,
            @AuthenticationPrincipal User hr) {
        testAssignmentService.assignTestToUsers(testId, request, hr.getId());
        return ResponseEntity.ok(ApiResponse.success("Тест назначен сотрудникам"));
    }

    @GetMapping("/tests/{testId}/assignments")
    public ResponseEntity<ApiResponse<List<TestAssignment>>> getTestAssignments(
            @PathVariable Long testId,
            @AuthenticationPrincipal User hr) {
        List<TestAssignment> assignments = testAssignmentService
                .getTestAssignments(testId, hr.getId());
        return ResponseEntity.ok(ApiResponse.success("Назначения получены", assignments));
    }

    // ========== ПРОВЕРКА РЕЗУЛЬТАТОВ ==========

    @GetMapping("/evaluation/open-answers")
    public ResponseEntity<ApiResponse<List<AnswerResponse>>> getOpenAnswersForEvaluation(
            @AuthenticationPrincipal User hr) {
        List<Answer> answers = evaluationService.getOpenAnswersForEvaluation(hr.getId());
        List<AnswerResponse> response = answers.stream()
                .map(answerMapper::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Открытые вопросы для проверки", response));
    }

    @PostMapping("/evaluation/answers/{answerId}")
    public ResponseEntity<ApiResponse<AnswerResponse>> evaluateAnswer(
            @PathVariable Long answerId,
            @Valid @RequestBody EvaluateAnswerRequest request,
            @AuthenticationPrincipal User hr) {
        Answer answer = evaluationService.evaluateAnswer(answerId, request, hr.getId());
        AnswerResponse response = answerMapper.toDto(answer);
        return ResponseEntity.ok(ApiResponse.success("Ответ оценен", response));
    }

    @GetMapping("/evaluation/attempts")
    public ResponseEntity<ApiResponse<List<AttemptResponse>>> getAttemptsForEvaluation(
            @AuthenticationPrincipal User hr) {
        List<Attempt> attempts = evaluationService.getAttemptsForEvaluation(hr.getId());
        List<AttemptResponse> response = attempts.stream()
                .map(attemptMapper::toAttemptResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Попытки на проверке", response));
    }

    @PostMapping("/evaluation/attempts/{attemptId}/complete")
    public ResponseEntity<ApiResponse<AttemptResponse>> completeEvaluation(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal User hr) {
        Attempt attempt = evaluationService.completeEvaluation(attemptId, hr.getId());
        AttemptResponse response = attemptMapper.toAttemptResponse(attempt);
        return ResponseEntity.ok(ApiResponse.success("Проверка завершена", response));
    }

    @GetMapping("/completed-attempts")
    public ResponseEntity<ApiResponse<List<AttemptResponse>>> getCompletedAttempts(
            @AuthenticationPrincipal User hr) {
        List<Attempt> allAttempts = attemptRepository.findAll();

        // Фильтруем завершенные попытки (COMPLETED или EVALUATED)
        List<Attempt> completedAttempts = allAttempts.stream()
                .filter(a -> a.getStatus() == AttemptStatus.COMPLETED ||
                        a.getStatus() == AttemptStatus.EVALUATED)
                .toList();

        List<AttemptResponse> response = completedAttempts.stream()
                .map(attemptMapper::toAttemptResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.success("Завершенные попытки", response));
    }

    @GetMapping("/attempts/{attemptId}/details")
    public ResponseEntity<ApiResponse<TestResultResponse>> getAttemptDetailsForHR(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal User hr) {
        // TODO: Реализовать детали попытки для HR
        return ResponseEntity.ok(ApiResponse.success("Детали попытки", null));
    }
}