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
import org.legend8883.competencytestingsystem.mapper.UserMapper;
import org.legend8883.competencytestingsystem.repository.AnswerRepository;
import org.legend8883.competencytestingsystem.repository.AttemptRepository;
import org.legend8883.competencytestingsystem.service.EvaluationService;
import org.legend8883.competencytestingsystem.service.TestAssignmentService;
import org.legend8883.competencytestingsystem.service.TestService;
import org.legend8883.competencytestingsystem.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final UserMapper userMapper;

    private final AttemptRepository attemptRepository;
    private final AnswerRepository answerRepository;

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

    // Эндпоинт для получения всех попыток
    @GetMapping("/attempts/all")
    public ResponseEntity<ApiResponse<List<AttemptResponse>>> getAllAttempts(
            @AuthenticationPrincipal User hr) {

        // Получаем все попытки
        List<Attempt> allAttempts = attemptRepository.findAll();

        // Фильтруем попытки, связанные с тестами этого HR
        List<Attempt> hrAttempts = allAttempts.stream()
                .filter(attempt ->
                        attempt.getTest() != null &&
                                attempt.getTest().getCreatedBy() != null &&
                                attempt.getTest().getCreatedBy().getId().equals(hr.getId())
                )
                .toList();

        List<AttemptResponse> response = hrAttempts.stream()
                .map(attemptMapper::toAttemptResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.success("Все попытки", response));
    }

    // Эндпоинт для получения деталей попытки
    @GetMapping("/attempts/{attemptId}/full")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAttemptFullDetails(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal User hr) {

        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        // Проверяем права HR
        if (!attempt.getTest().getCreatedBy().getId().equals(hr.getId())) {
            throw new RuntimeException("Access denied");
        }

        Map<String, Object> response = new HashMap<>();

        // Базовые данные попытки
        response.put("attemptId", attempt.getId());
        response.put("testId", attempt.getTest().getId());
        response.put("testTitle", attempt.getTest().getTitle());
        response.put("user", userMapper.toDto(attempt.getUser()));
        response.put("startedAt", attempt.getStartedAt());
        response.put("completedAt", attempt.getCompletedAt());
        response.put("score", attempt.getScore());
        response.put("status", attempt.getStatus().name());
        response.put("passed", attempt.getScore() != null &&
                attempt.getTest().getPassingScore() != null &&
                attempt.getScore() >= attempt.getTest().getPassingScore());
        response.put("maxPossibleScore", attempt.getTest().getMaxPossibleScore());
        response.put("passingScore", attempt.getTest().getPassingScore());

        // Загружаем ответы с вопросами
        List<Answer> answers = answerRepository.findByAttemptWithQuestions(attempt);

        // Подсчитываем правильные ответы
        long correctAnswersCount = answers.stream()
                .filter(this::isAnswerCorrect)
                .count();

        response.put("correctAnswersCount", (int) correctAnswersCount);
        response.put("totalQuestions", attempt.getTest().getQuestions().size());

        // Маппим ответы
        List<Map<String, Object>> answerResponses = answers.stream()
                .map(this::mapAnswerToFullMap)
                .toList();

        response.put("answers", answerResponses);

        return ResponseEntity.ok(ApiResponse.success("Детали попытки", response));
    }

    // Вспомогательный метод для маппинга ответа
    private Map<String, Object> mapAnswerToFullMap(Answer answer) {
        Map<String, Object> map = new HashMap<>();

        map.put("questionId", answer.getQuestion().getId());
        map.put("questionText", answer.getQuestion().getText());
        map.put("questionType", answer.getQuestion().getType().name());
        map.put("questionMaxScore", answer.getQuestion().getMaxScore());
        map.put("openAnswerText", answer.getOpenAnswerText());
        map.put("assignedScore", answer.getAssignedScore());
        map.put("autoScore", answer.getAutoScore());

        // Проверяем правильность ответа
        boolean isCorrect = isAnswerCorrect(answer);
        map.put("isCorrect", isCorrect);

        // Для вопросов с выбором
        if (answer.getQuestion().getType() == QuestionType.SINGLE_CHOICE ||
                answer.getQuestion().getType() == QuestionType.MULTIPLE_CHOICE) {

            List<Map<String, Object>> options = new ArrayList<>();

            for (AnswerOption option : answer.getQuestion().getOptions()) {
                Map<String, Object> optionMap = new HashMap<>();
                optionMap.put("id", option.getId());
                optionMap.put("text", option.getText());
                optionMap.put("isCorrect", option.getIsCorrect());

                // Помечаем выбранные варианты
                boolean isSelected = answer.getSelectedOptionIds() != null &&
                        answer.getSelectedOptionIds().contains(option.getId());
                optionMap.put("selected", isSelected);

                options.add(optionMap);
            }

            map.put("selectedOptions", options);
        }

        return map;
    }

    // Вспомогательный метод для проверки правильности ответа
    private boolean isAnswerCorrect(Answer answer) {
        if (answer.getQuestion().getType() == QuestionType.OPEN_ANSWER) {
            // Для открытых вопросов правильность определяется HR
            return answer.getAssignedScore() != null &&
                    answer.getAssignedScore() > 0;
        } else if (answer.getQuestion().getType() == QuestionType.SINGLE_CHOICE) {
            // Для одиночного выбора
            if (answer.getSelectedOptionIds() == null || answer.getSelectedOptionIds().size() != 1) {
                return false;
            }

            Long selectedId = answer.getSelectedOptionIds().get(0);
            return answer.getQuestion().getOptions().stream()
                    .filter(AnswerOption::getIsCorrect)
                    .anyMatch(option -> option.getId().equals(selectedId));
        } else if (answer.getQuestion().getType() == QuestionType.MULTIPLE_CHOICE) {
            // Для множественного выбора
            List<Long> correctOptionIds = answer.getQuestion().getOptions().stream()
                    .filter(AnswerOption::getIsCorrect)
                    .map(AnswerOption::getId)
                    .toList();

            if (answer.getSelectedOptionIds() == null) {
                return false;
            }

            // Все правильные выбраны и нет неправильных
            return answer.getSelectedOptionIds().containsAll(correctOptionIds) &&
                    correctOptionIds.containsAll(answer.getSelectedOptionIds());
        }

        return false;
    }


    private AnswerFullResponse mapToAnswerFullResponse(Answer answer) {
        AnswerFullResponse response = new AnswerFullResponse();
        response.setId(answer.getId());
        response.setQuestionId(answer.getQuestion().getId());
        response.setQuestionText(answer.getQuestion().getText());
        response.setQuestionType(answer.getQuestion().getType().name());
        response.setMaxScore(answer.getQuestion().getMaxScore());
        response.setOpenAnswerText(answer.getOpenAnswerText());
        response.setAssignedScore(answer.getAssignedScore());
        response.setAutoScore(answer.getAutoScore());

        // Для вопросов с выбором
        if (answer.getQuestion().isChoiceQuestion()) {
            List<AnswerOptionWithSelectionResponse> allOptions = new ArrayList<>();

            for (AnswerOption option : answer.getQuestion().getOptions()) {
                AnswerOptionWithSelectionResponse optionResponse = new AnswerOptionWithSelectionResponse();
                optionResponse.setId(option.getId());
                optionResponse.setText(option.getText());
                optionResponse.setIsCorrect(option.getIsCorrect());
                optionResponse.setOrderIndex(option.getOrderIndex());

                // Помечаем выбранные варианты
                boolean isSelected = answer.getSelectedOptionIds() != null &&
                        answer.getSelectedOptionIds().contains(option.getId());
                optionResponse.setSelected(isSelected);

                allOptions.add(optionResponse);
            }

            response.setSelectedOptions(allOptions);

            // Определяем правильность
            boolean isCorrect;

            if (answer.getQuestion().getType() == QuestionType.SINGLE_CHOICE) {
                // Для одиночного выбора
                isCorrect = allOptions.stream()
                        .anyMatch(option -> option.getSelected() && option.getIsCorrect());
            } else {
                // Для множественного выбора
                boolean allCorrectSelected = allOptions.stream()
                        .filter(AnswerOptionWithSelectionResponse::getIsCorrect)
                        .allMatch(AnswerOptionWithSelectionResponse::getSelected);

                boolean noIncorrectSelected = allOptions.stream()
                        .filter(option -> !option.getIsCorrect())
                        .noneMatch(AnswerOptionWithSelectionResponse::getSelected);

                isCorrect = allCorrectSelected && noIncorrectSelected;
            }

            response.setCorrect(isCorrect);
        } else {
            // Для открытых вопросов
            response.setCorrect(answer.getAssignedScore() != null &&
                    answer.getAssignedScore() > 0);
        }

        return response;
    }


}