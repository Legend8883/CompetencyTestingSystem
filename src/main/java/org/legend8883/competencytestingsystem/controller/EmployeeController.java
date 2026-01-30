package org.legend8883.competencytestingsystem.controller;

import lombok.RequiredArgsConstructor;
import org.legend8883.competencytestingsystem.dto.request.StartTestRequest;
import org.legend8883.competencytestingsystem.dto.request.SubmitAnswerRequest;
import org.legend8883.competencytestingsystem.dto.response.*;
import org.legend8883.competencytestingsystem.entity.*;
import org.legend8883.competencytestingsystem.mapper.AttemptMapper;
import org.legend8883.competencytestingsystem.mapper.TestMapper;
import org.legend8883.competencytestingsystem.mapper.UserMapper;
import org.legend8883.competencytestingsystem.repository.AnswerRepository;
import org.legend8883.competencytestingsystem.repository.AttemptRepository;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private final TestAssignmentService testAssignmentService;
    private final AttemptService attemptService;
    private final TestMapper testMapper;
    private final AttemptMapper attemptMapper;
    private final UserMapper userMapper;

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final AttemptRepository attemptRepository;
    private final AnswerRepository answerRepository;

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
    public ResponseEntity<ApiResponse<List<AttemptResponse>>> getMyAttempts(
            @AuthenticationPrincipal User employee) {

        List<Attempt> attempts = attemptRepository.findByUser(employee);

        // Создаем DTO с информацией об открытых вопросах
        List<AttemptResponse> response = attempts.stream()
                .map(this::createAttemptResponseWithDetails)
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Мои попытки", response));
    }

    // Метод для проверки наличия открытых вопросов в тесте
    private boolean hasTestOpenQuestions(Test test) {
        if (test == null || test.getQuestions() == null) {
            return false;
        }

        return test.getQuestions().stream()
                .anyMatch(q -> q.getType() == QuestionType.OPEN_ANSWER);
    }

    // Метод для создания полного DTO попытки с информацией об открытых вопросах
    private AttemptResponse createAttemptResponseWithDetails(Attempt attempt) {
        AttemptResponse dto = attemptMapper.toAttemptResponse(attempt);

        // Используем рефлексию для установки поля, или создадим новый DTO
        try {
            // Попробуем установить поле через рефлексию
            java.lang.reflect.Field field = dto.getClass().getDeclaredField("hasOpenQuestions");
            field.setAccessible(true);
            field.set(dto, hasTestOpenQuestions(attempt.getTest()));
        } catch (Exception e) {
            // Если поля нет, создадим обертку
            System.out.println("Field hasOpenQuestions not found in AttemptResponse");
        }

        return dto;
    }

    // Метод для создания результатов теста (добавить в EmployeeController)
    private TestResultResponse createTestResultResponse(Attempt attempt) {
        TestResultResponse response = new TestResultResponse();

        // Базовые поля
        response.setAttemptId(attempt.getId());
        response.setTestId(attempt.getTest().getId());
        response.setTestTitle(attempt.getTest().getTitle());
        response.setUser(userMapper.toDto(attempt.getUser()));
        response.setStartedAt(attempt.getStartedAt());
        response.setCompletedAt(attempt.getCompletedAt());
        response.setScore(attempt.getScore());
        response.setStatus(attempt.getStatus().name());
        response.setPassingScore(attempt.getTest().getPassingScore());
        response.setMaxPossibleScore(attempt.getTest().getMaxPossibleScore());

        // Рассчитываем прошел/не прошел
        boolean passed = attempt.getScore() != null &&
                attempt.getTest().getPassingScore() != null &&
                attempt.getScore() >= attempt.getTest().getPassingScore();
        response.setPassed(passed);

        // Загружаем ответы с вопросами
        List<Answer> answers = answerRepository.findByAttemptWithQuestions(attempt);

        // Подсчитываем правильные ответы
        long correctAnswersCount = answers.stream()
                .filter(this::isAnswerCorrect)
                .count();

        response.setCorrectAnswersCount((int) correctAnswersCount);
        response.setTotalQuestions(attempt.getTest().getQuestions().size());

        // Создаем детали ответов
        List<AnswerResultResponse> answerResponses = answers.stream()
                .map(this::createAnswerResultResponse)
                .toList();

        response.setAnswers(answerResponses);

        return response;
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

    // Вспомогательный метод для создания DTO результата ответа
    private AnswerResultResponse createAnswerResultResponse(Answer answer) {
        AnswerResultResponse response = new AnswerResultResponse();

        response.setQuestionId(answer.getQuestion().getId());
        response.setQuestionText(answer.getQuestion().getText());
        response.setQuestionType(answer.getQuestion().getType().name());
        response.setQuestionMaxScore(answer.getQuestion().getMaxScore());

        if (answer.getQuestion().getType() == QuestionType.OPEN_ANSWER) {
            response.setUserAnswer(answer.getOpenAnswerText());
        } else {
            // Для вопросов с выбором
            List<AnswerOptionResponse> selectedOptions = new ArrayList<>();

            if (answer.getSelectedOptionIds() != null) {
                for (Long optionId : answer.getSelectedOptionIds()) {
                    AnswerOption option = answer.getQuestion().getOptions().stream()
                            .filter(o -> o.getId().equals(optionId))
                            .findFirst()
                            .orElse(null);

                    if (option != null) {
                        AnswerOptionResponse optionResponse = new AnswerOptionResponse();
                        optionResponse.setId(option.getId());
                        optionResponse.setText(option.getText());
                        optionResponse.setIsCorrect(option.getIsCorrect());
                        optionResponse.setOrderIndex(option.getOrderIndex());
                        selectedOptions.add(optionResponse);
                    }
                }
            }

            response.setSelectedOptions(selectedOptions);
        }

        // Устанавливаем баллы
        if (answer.getAssignedScore() != null) {
            response.setAssignedScore(answer.getAssignedScore());
            response.setFinalScore(answer.getAssignedScore());
        } else {
            response.setAutoScore(answer.getAutoScore());
            response.setFinalScore(answer.getAutoScore());
        }

        response.setIsCorrect(isAnswerCorrect(answer));

        return response;
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

    @GetMapping("/attempts/{attemptId}")
    public ResponseEntity<ApiResponse<TestResultResponse>> getAttemptDetails(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal User employee) {
        // TODO: Реализовать получение деталей попытки
        // Нужно будет создать новый сервис или расширить существующий
        return ResponseEntity.ok(ApiResponse.success("Детали попытки", null));
    }

    @GetMapping("/attempts/all")
    public ResponseEntity<ApiResponse<List<AttemptResponse>>> getAllAttempts(
            @AuthenticationPrincipal User employee) {
        List<Attempt> attempts = attemptRepository.findByUser(employee);
        List<AttemptResponse> response = attempts.stream()
                .map(attemptMapper::toAttemptResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Все попытки", response));
    }

    // ПЕРЕЙТИ К КОНКРЕТНОМУ ВОПРОСУ
    @GetMapping("/attempts/{attemptId}/questions")
    public ResponseEntity<ApiResponse<List<QuestionWithAnswerResponse>>> getAllQuestionsForAttempt(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal User employee) {

        List<QuestionWithAnswerResponse> questions = attemptService.getAllQuestionsForAttemptWithAnswers(attemptId, employee.getId());
        return ResponseEntity.ok(ApiResponse.success("Все вопросы с ответами", questions));
    }

    // Получить конкретный вопрос
    @GetMapping("/attempts/{attemptId}/questions/{questionId}")
    public ResponseEntity<ApiResponse<QuestionWithAnswerResponse>> getQuestion(
            @PathVariable Long attemptId,
            @PathVariable Long questionId,
            @AuthenticationPrincipal User employee) {

        QuestionWithAnswerResponse question = attemptService.getQuestionWithAnswer(attemptId, questionId, employee.getId());
        return ResponseEntity.ok(ApiResponse.success("Вопрос с ответом", question));
    }

    // Перейти к конкретному вопросу
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
    public ResponseEntity<ApiResponse<TestResultResponse>> getAttemptResults(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal User employee) {

        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        if (!attempt.getUser().getId().equals(employee.getId())) {
            throw new RuntimeException("This attempt belongs to another user");
        }

        TestResultResponse response = createTestResultResponse(attempt);
        return ResponseEntity.ok(ApiResponse.success("Результаты теста", response));
    }

    // Эндпоинт для получения попытки с ответами
    @GetMapping("/attempts/{attemptId}/with-answers")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAttemptWithAnswers(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal User employee) {

        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        if (!attempt.getUser().getId().equals(employee.getId())) {
            throw new RuntimeException("This attempt belongs to another user");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", attempt.getId());
        response.put("testId", attempt.getTest().getId());
        response.put("testTitle", attempt.getTest().getTitle());
        response.put("status", attempt.getStatus().name());
        response.put("completedAt", attempt.getCompletedAt());
        response.put("score", attempt.getScore());

        // Рассчитываем автооценку
        List<Answer> answers = answerRepository.findByAttempt(attempt);
        Integer autoScore = answers.stream()
                .mapToInt(answer -> answer.getAutoScore() != null ? answer.getAutoScore() : 0)
                .sum();
        response.put("autoScore", autoScore);

        // Загружаем ответы с вопросами
        List<Answer> fullAnswers = answerRepository.findByAttemptWithQuestions(attempt);

        List<Map<String, Object>> answerResponses = fullAnswers.stream()
                .map(this::mapAnswerToSimpleMap)
                .toList();

        response.put("answers", answerResponses);

        return ResponseEntity.ok(ApiResponse.success("Детали попытки", response));
    }

    // Вспомогательный метод для маппинга ответа
    private Map<String, Object> mapAnswerToSimpleMap(Answer answer) {
        Map<String, Object> map = new HashMap<>();

        map.put("questionId", answer.getQuestion().getId());
        map.put("questionText", answer.getQuestion().getText());
        map.put("questionType", answer.getQuestion().getType().name());
        map.put("maxScore", answer.getQuestion().getMaxScore());
        map.put("openAnswerText", answer.getOpenAnswerText());
        map.put("assignedScore", answer.getAssignedScore());
        map.put("autoScore", answer.getAutoScore());

        // Для вопросов с выбором
        if (answer.getQuestion().getType() == QuestionType.SINGLE_CHOICE ||
                answer.getQuestion().getType() == QuestionType.MULTIPLE_CHOICE) {

            List<Map<String, Object>> options = new ArrayList<>();

            for (AnswerOption option : answer.getQuestion().getOptions()) {
                Map<String, Object> optionMap = new HashMap<>();
                optionMap.put("id", option.getId());
                optionMap.put("text", option.getText());

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

    // Вспомогательный метод для расчета автооценки
    private Integer calculateAutoScore(Attempt attempt) {
        List<Answer> answers = answerRepository.findByAttempt(attempt);

        return answers.stream()
                .mapToInt(answer -> answer.getAutoScore() != null ? answer.getAutoScore() : 0)
                .sum();
    }

    // Вспомогательный метод для создания DTO ответа с деталями
    private AnswerWithDetailsResponse createAnswerWithDetailsResponse(Answer answer) {
        AnswerWithDetailsResponse response = new AnswerWithDetailsResponse();

        response.setQuestionId(answer.getQuestion().getId());
        response.setQuestionText(answer.getQuestion().getText());
        response.setQuestionType(answer.getQuestion().getType().name());
        response.setMaxScore(answer.getQuestion().getMaxScore());
        response.setOpenAnswerText(answer.getOpenAnswerText());
        response.setAssignedScore(answer.getAssignedScore());
        response.setAutoScore(answer.getAutoScore());

        // Для вопросов с выбором
        if (answer.getQuestion().getType() == QuestionType.SINGLE_CHOICE ||
                answer.getQuestion().getType() == QuestionType.MULTIPLE_CHOICE) {

            List<AnswerOptionSimpleResponse> selectedOptions = new ArrayList<>();

            if (answer.getSelectedOptionIds() != null) {
                for (Long optionId : answer.getSelectedOptionIds()) {
                    AnswerOption option = answer.getQuestion().getOptions().stream()
                            .filter(o -> o.getId().equals(optionId))
                            .findFirst()
                            .orElse(null);

                    if (option != null) {
                        AnswerOptionSimpleResponse optionResponse = new AnswerOptionSimpleResponse();
                        optionResponse.setId(option.getId());
                        optionResponse.setText(option.getText());
                        selectedOptions.add(optionResponse);
                    }
                }
            }

            response.setSelectedOptions(selectedOptions);
        }

        return response;
    }
}
