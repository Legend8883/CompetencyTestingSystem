package org.legend8883.competencytestingsystem.service;

import lombok.RequiredArgsConstructor;
import org.legend8883.competencytestingsystem.dto.request.StartTestRequest;
import org.legend8883.competencytestingsystem.dto.request.SubmitAnswerRequest;
import org.legend8883.competencytestingsystem.dto.response.*;
import org.legend8883.competencytestingsystem.entity.*;
import org.legend8883.competencytestingsystem.mapper.AnswerOptionMapper;
import org.legend8883.competencytestingsystem.mapper.AttemptMapper;
import org.legend8883.competencytestingsystem.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttemptService {

    private final AttemptRepository attemptRepository;
    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final TestAssignmentRepository testAssignmentRepository;

    // Начать тестирование
    @Transactional
    public TestProgressResponse startTest(StartTestRequest request, Long employeeId) {
        // 1. Проверить что тест назначен сотруднику
        User employee = new User();
        employee.setId(employeeId);

        Test test = testRepository.findById(request.getTestId())
                .orElseThrow(() -> new RuntimeException("Test not found"));

        // Проверить назначение
        Optional<TestAssignment> assignment = testAssignmentRepository
                .findByUserAndTest(employee, test);

        if (assignment.isEmpty()) {
            throw new RuntimeException("Test is not assigned to you");
        }

        if (!assignment.get().getIsActive()) {
            throw new RuntimeException("Test assignment is not active");
        }

        if (assignment.get().getDeadline() != null &&
                assignment.get().getDeadline().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Test deadline has passed");
        }

        // 2. Проверить нет ли активной попытки
        Optional<Attempt> activeAttempt = attemptRepository
                .findByUserAndStatus(employee, AttemptStatus.IN_PROGRESS)
                .stream()
                .filter(a -> a.getTest().getId().equals(test.getId()))
                .findFirst();

        if (activeAttempt.isPresent()) {
            // Возвращаем существующую попытку
            return createProgressResponse(activeAttempt.get());
        }

        // 3. Создать новую попытку
        Attempt attempt = new Attempt();
        attempt.setUser(employee);
        attempt.setTest(test);
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt.setScore(0);

        Attempt savedAttempt = attemptRepository.save(attempt);

        // 4. Создать пустые ответы для всех вопросов
        List<Question> questions = questionRepository.findByTestWithOptions(test);
        List<Answer> answers = new ArrayList<>();

        for (Question question : questions) {
            Answer answer = new Answer();
            answer.setAttempt(savedAttempt);
            answer.setQuestion(question);
            answers.add(answer);
        }

        answerRepository.saveAll(answers);

        return createProgressResponse(savedAttempt);
    }

    // Отправить ответ на вопрос
    @Transactional
    public TestProgressResponse submitAnswer(Long attemptId, SubmitAnswerRequest request, Long employeeId) {
        // 1. Найти попытку
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        // 2. Проверить права
        if (!attempt.getUser().getId().equals(employeeId)) {
            throw new RuntimeException("This attempt belongs to another user");
        }

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new RuntimeException("Attempt is not in progress");
        }

        // 3. Найти вопрос
        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new RuntimeException("Question not found"));

        // 4. Найти или создать ответ
        Answer answer = answerRepository.findByAttemptAndQuestion(attempt, question)
                .orElseGet(() -> {
                    Answer newAnswer = new Answer();
                    newAnswer.setAttempt(attempt);
                    newAnswer.setQuestion(question);
                    return newAnswer;
                });

        // 5. Сохранить ответ в зависимости от типа вопроса
        if (question.getType() == QuestionType.SINGLE_CHOICE ||
                question.getType() == QuestionType.MULTIPLE_CHOICE) {

            // Валидация выбранных вариантов
            if (request.getSelectedOptionIds() != null) {
                for (Long optionId : request.getSelectedOptionIds()) {
                    AnswerOption option = answerOptionRepository.findById(optionId)
                            .orElseThrow(() -> new RuntimeException("Option not found: " + optionId));

                    if (!option.getQuestion().getId().equals(question.getId())) {
                        throw new RuntimeException("Option does not belong to this question");
                    }
                }

                answer.setSelectedOptionIds(request.getSelectedOptionIds());

                // Автоматическая проверка для вопросов с выбором
                if (question.getType() == QuestionType.SINGLE_CHOICE) {
                    calculateScoreForSingleChoice(answer, question);
                } else if (question.getType() == QuestionType.MULTIPLE_CHOICE) {
                    calculateScoreForMultipleChoice(answer, question);
                }
            }

        } else if (question.getType() == QuestionType.OPEN_ANSWER) {
            // Для открытых вопросов
            if (request.getOpenAnswerText() != null &&
                    request.getOpenAnswerText().length() > 5000) {
                throw new RuntimeException("Answer is too long (max 5000 characters)");
            }

            answer.setOpenAnswerText(request.getOpenAnswerText());
            // Открытые вопросы проверяются HR вручную
            answer.setAutoScore(0);
        }

        answer.setAnsweredAt(LocalDateTime.now());
        answerRepository.save(answer);

        return createProgressResponse(attempt);
    }

    // Завершить тест
    @Transactional
    public TestProgressResponse completeTest(Long attemptId, Long employeeId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        if (!attempt.getUser().getId().equals(employeeId)) {
            throw new RuntimeException("This attempt belongs to another user");
        }

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new RuntimeException("Attempt is not in progress");
        }

        // Рассчитать итоговый балл
        calculateFinalScore(attempt);

        // Определить новый статус
        boolean hasOpenQuestions = hasOpenQuestions(attempt);

        // ВАЖНО: Используйте только те статусы, которые есть в ENUM
        if (hasOpenQuestions) {
            // Используйте EVALUATING вместо SUBMITTED
            attempt.setStatus(AttemptStatus.EVALUATING);
        } else {
            attempt.setStatus(AttemptStatus.COMPLETED);
        }

        attempt.setCompletedAt(LocalDateTime.now());

        try {
            attemptRepository.save(attempt);
            System.out.println("Test completed successfully with status: " + attempt.getStatus());
        } catch (Exception e) {
            System.err.println("ERROR saving attempt: " + e.getMessage());
            throw new RuntimeException("Failed to save attempt: " + e.getMessage());
        }

        return createProgressResponse(attempt);
    }

    // Получить прогресс теста
    public TestProgressResponse getTestProgress(Long attemptId, Long employeeId) {
        System.out.println("=== getTestProgress called ===");
        System.out.println("Attempt ID: " + attemptId);
        System.out.println("Employee ID: " + employeeId);

        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        if (!attempt.getUser().getId().equals(employeeId)) {
            throw new RuntimeException("This attempt belongs to another user");
        }

        System.out.println("Attempt found. Status: " + attempt.getStatus());

        TestProgressResponse response = createProgressResponse(attempt);

        System.out.println("Response created. CurrentQuestion is null? " +
                (response.getCurrentQuestion() == null));
        System.out.println("=== getTestProgress end ===");

        return response;
    }

    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ

    private TestProgressResponse createProgressResponse(Attempt attempt) {
        TestProgressResponse response = new TestProgressResponse();

        try {
            System.out.println("=== DEBUG createProgressResponse ===");
            System.out.println("Attempt ID: " + attempt.getId());
            System.out.println("Test ID: " + attempt.getTest().getId());

            // Базовые поля
            response.setAttemptId(attempt.getId());
            response.setTestId(attempt.getTest().getId());
            response.setTestTitle(attempt.getTest().getTitle());
            response.setStartedAt(attempt.getStartedAt());
            response.setAutoSubmitAt(attempt.getAutoSubmitAt());
            response.setTimeLeftMinutes(calculateTimeLeft(attempt));

            // Получаем вопросы теста
            List<Question> questions = questionRepository.findByTestWithOptions(attempt.getTest());
            System.out.println("Questions loaded: " + questions.size());

            if (questions.isEmpty()) {
                System.out.println("WARNING: No questions found for test!");
            }

            response.setTotalQuestions(questions.size());

            // ВАЖНО: Загружаем ответы один раз для всей попытки
            List<Answer> answers = answerRepository.findByAttemptWithQuestions(attempt);
            System.out.println("Answers loaded: " + answers.size());

            // Создаем прогресс вопросов
            List<QuestionProgressResponse> questionProgress = createQuestionProgress(questions, answers);
            response.setQuestionProgress(questionProgress);

            // Находим текущий вопрос
            if (!questions.isEmpty()) {
                // НАХОДИМ ТЕКУЩИЙ ВОПРОС НА ОСНОВЕ ОТВЕТОВ
                int currentIndex = findCurrentQuestionIndex(questions, answers);
                Question question = questions.get(currentIndex);

                response.setCurrentQuestionIndex(currentIndex);

                // ПЕРЕДАЕМ ТРИ АРГУМЕНТА: question, attempt И answers
                QuestionWithAnswerResponse questionResponse = createSimpleQuestionResponse(question, attempt, answers);
                response.setCurrentQuestion(questionResponse);

                System.out.println("Current question: ID=" + question.getId() +
                        ", Index=" + currentIndex +
                        ", Type=" + question.getType());
            } else {
                response.setCurrentQuestionIndex(0);
                System.out.println("WARNING: No questions available!");
            }

            System.out.println("CurrentQuestion is null? " + (response.getCurrentQuestion() == null));
            System.out.println("=== END DEBUG ===");

        } catch (Exception e) {
            System.err.println("ERROR in createProgressResponse: " + e.getMessage());
            e.printStackTrace();
        }

        // Запасной вариант: если currentQuestion все еще null
        if (response.getCurrentQuestion() == null && response.getTotalQuestions() > 0) {
            try {
                List<Question> questions = questionRepository.findByTestWithOptions(attempt.getTest());
                List<Answer> answers = answerRepository.findByAttemptWithQuestions(attempt);

                if (!questions.isEmpty()) {
                    int currentIndex = findCurrentQuestionIndex(questions, answers);
                    Question question = questions.get(currentIndex);
                    response.setCurrentQuestionIndex(currentIndex);

                    QuestionWithAnswerResponse questionResponse = createSimpleQuestionResponse(question, attempt, answers);
                    response.setCurrentQuestion(questionResponse);

                    System.out.println("FORCE SET currentQuestion to index: " + currentIndex);
                }
            } catch (Exception e) {
                System.err.println("Failed to force set currentQuestion: " + e.getMessage());
            }
        }

        return response;
    }

    private QuestionWithAnswerResponse createSimpleQuestionResponse(
            Question question, Attempt attempt, List<Answer> answers) {

        System.out.println("=== DEBUG createSimpleQuestionResponse ===");
        System.out.println("Question ID: " + question.getId());
        System.out.println("Question Type: " + question.getType());
        System.out.println("Question Text: " + (question.getText() != null ? question.getText().substring(0, Math.min(50, question.getText().length())) : "null"));

        // Создаем базовый DTO
        QuestionWithAnswerResponse response = new QuestionWithAnswerResponse();
        response.setId(question.getId());
        response.setText(question.getText());
        response.setType(question.getType().name());
        response.setOrderIndex(question.getOrderIndex());

        // 1. ОБРАБОТКА ВАРИАНТОВ ОТВЕТОВ (только для CHOICE вопросов)
        if (question.getType() == QuestionType.SINGLE_CHOICE ||
                question.getType() == QuestionType.MULTIPLE_CHOICE) {

            System.out.println("Processing options for CHOICE question");

            if (question.getOptions() != null && !question.getOptions().isEmpty()) {
                List<AnswerOptionResponse> options = new ArrayList<>();

                for (AnswerOption option : question.getOptions()) {
                    AnswerOptionResponse optionResponse = new AnswerOptionResponse();
                    optionResponse.setId(option.getId());
                    optionResponse.setText(option.getText());
                    optionResponse.setOrderIndex(option.getOrderIndex());
                    optionResponse.setIsCorrect(false); // НЕ показываем правильность пользователю

                    options.add(optionResponse);
                    System.out.println("Added option: ID=" + option.getId() +
                            ", Text length=" + (option.getText() != null ? option.getText().length() : 0));
                }

                response.setOptions(options);
                System.out.println("Total options in DTO: " + options.size());
            } else {
                System.out.println("WARNING: CHOICE question has no options!");
                response.setOptions(new ArrayList<>());
            }
        } else if (question.getType() == QuestionType.OPEN_ANSWER) {
            // Для открытых вопросов оставляем пустой список или null
            response.setOptions(null);
            System.out.println("OPEN_ANSWER question - no options needed");
        }

        // 2. ПОЛУЧЕНИЕ СОХРАНЕННОГО ОТВЕТА (если есть)
        // Ищем ответ для этого вопроса в текущей попытке
        Optional<Answer> existingAnswer = answers.stream()
                .filter(a -> a.getQuestion() != null &&
                        a.getQuestion().getId().equals(question.getId()))
                .findFirst();

        System.out.println("Existing answer found: " + existingAnswer.isPresent());

        if (existingAnswer.isPresent()) {
            Answer answer = existingAnswer.get();

            if (question.getType() == QuestionType.OPEN_ANSWER) {
                // Для открытых вопросов
                String openAnswer = answer.getOpenAnswerText();
                response.setPreviousAnswer(openAnswer);
                System.out.println("Open answer restored: " +
                        (openAnswer != null ? openAnswer.substring(0, Math.min(50, openAnswer.length())) + "..." : "null"));

            } else if (question.getType() == QuestionType.SINGLE_CHOICE ||
                    question.getType() == QuestionType.MULTIPLE_CHOICE) {
                // Для вопросов с выбором
                List<Long> selectedOptionIds = answer.getSelectedOptionIds();
                response.setPreviousSelectedOptions(selectedOptionIds);

                System.out.println("Selected options restored: " +
                        (selectedOptionIds != null ? selectedOptionIds.toString() : "null"));

                // Также обновляем состояние чекбоксов/радиокнопок во фронтенде
                // (это делается через установку атрибута checked)
            }
        } else {
            // Нет сохраненного ответа
            System.out.println("No saved answer for this question");
            response.setPreviousAnswer(null);
            response.setPreviousSelectedOptions(null);
        }

        System.out.println("=== END DEBUG createSimpleQuestionResponse ===");
        return response;
    }

    private Integer calculateTimeLeft(Attempt attempt) {
        if (attempt.getAutoSubmitAt() == null || attempt.getCompletedAt() != null) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(attempt.getAutoSubmitAt())) {
            return 0;
        }

        return (int) java.time.Duration.between(now, attempt.getAutoSubmitAt()).toMinutes();
    }

    private void calculateScoreForSingleChoice(Answer answer, Question question) {
        if (answer.getSelectedOptionIds().size() != 1) {
            answer.setAutoScore(0);
            return;
        }

        Long selectedId = answer.getSelectedOptionIds().get(0);
        AnswerOption selectedOption = answerOptionRepository.findById(selectedId)
                .orElseThrow();

        answer.setAutoScore(selectedOption.getIsCorrect() ? question.getMaxScore() : 0);
    }

    private void calculateScoreForMultipleChoice(Answer answer, Question question) {
        List<AnswerOption> correctOptions = answerOptionRepository
                .findByQuestionAndIsCorrectTrue(question);

        List<Long> correctOptionIds = correctOptions.stream()
                .map(AnswerOption::getId)
                .toList();

        long correctSelected = answer.getSelectedOptionIds().stream()
                .filter(correctOptionIds::contains)
                .count();

        long incorrectSelected = answer.getSelectedOptionIds().stream()
                .filter(id -> !correctOptionIds.contains(id))
                .count();

        // Формула: (правильные - неправильные) / всего правильных * maxScore
        if (correctOptions.isEmpty()) {
            answer.setAutoScore(0);
        } else {
            double score = Math.max(0, (correctSelected - incorrectSelected))
                    / (double) correctOptions.size()
                    * question.getMaxScore();
            answer.setAutoScore((int) Math.round(score));
        }
    }

    private void calculateFinalScore(Attempt attempt) {
        List<Answer> answers = answerRepository.findByAttempt(attempt);

        int totalScore = answers.stream()
                .mapToInt(answer -> {
                    if (answer.getAssignedScore() != null) {
                        return answer.getAssignedScore();
                    }
                    return answer.getAutoScore() != null ? answer.getAutoScore() : 0;
                })
                .sum();

        attempt.setScore(totalScore);
    }

    private boolean hasOpenQuestions(Attempt attempt) {
        List<Answer> answers = answerRepository.findByAttempt(attempt);
        return answers.stream()
                .anyMatch(answer -> {
                    Question question = answer.getQuestion();
                    return question.getType() == QuestionType.OPEN_ANSWER &&
                            answer.getAssignedScore() == null;
                });
    }

    private List<QuestionProgressResponse> createQuestionProgress(
            List<Question> questions, List<Answer> answers) {

        List<QuestionProgressResponse> progress = new ArrayList<>();

        if (questions == null || questions.isEmpty()) {
            return progress; // Пустой список, а не null
        }

        for (Question question : questions) {
            QuestionProgressResponse progressItem = new QuestionProgressResponse();
            progressItem.setQuestionId(question.getId());
            progressItem.setOrderIndex(question.getOrderIndex());

            // Проверяем, отвечен ли вопрос
            boolean answered = answers.stream()
                    .anyMatch(answer ->
                            answer.getQuestion().getId().equals(question.getId()) &&
                                    answer.getAnsweredAt() != null);

            progressItem.setAnswered(answered);
            progressItem.setVisited(true);

            progress.add(progressItem);
        }

        return progress;
    }

    private Optional<Question> findCurrentQuestion(List<Question> questions, List<Answer> answers) {
        // Находим первый вопрос без ответа
        return questions.stream()
                .filter(question -> {
                    boolean hasAnswer = answers.stream()
                            .anyMatch(answer ->
                                    answer.getQuestion().getId().equals(question.getId()) &&
                                            answer.getAnsweredAt() != null);
                    return !hasAnswer; // Возвращаем вопросы без ответов
                })
                .findFirst();
    }

    private int findCurrentQuestionIndex(List<Question> questions, List<Answer> answers) {
        if (questions == null || questions.isEmpty()) {
            return 0;
        }

        System.out.println("=== DEBUG findCurrentQuestionIndex ===");
        System.out.println("Questions count: " + questions.size());

        // ЛОГИКА: Находим первый вопрос, на который еще не отвечали
        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);

            // Ищем ответ для этого вопроса
            boolean hasAnsweredAnswer = false;
            for (Answer answer : answers) {
                if (answer.getQuestion() != null &&
                        answer.getQuestion().getId().equals(question.getId())) {

                    // Проверяем, есть ли хоть какой-то ответ
                    boolean hasAnswerContent = false;
                    if (question.getType() == QuestionType.OPEN_ANSWER) {
                        hasAnswerContent = answer.getOpenAnswerText() != null &&
                                !answer.getOpenAnswerText().trim().isEmpty();
                    } else {
                        hasAnswerContent = answer.getSelectedOptionIds() != null &&
                                !answer.getSelectedOptionIds().isEmpty();
                    }

                    if (hasAnswerContent) {
                        hasAnsweredAnswer = true;
                        break;
                    }
                }
            }

            System.out.println("Question " + (i+1) + " (ID=" + question.getId() +
                    "): hasAnsweredAnswer=" + hasAnsweredAnswer);

            if (!hasAnsweredAnswer) {
                System.out.println("First unanswered question found at index: " + i);
                System.out.println("=== END DEBUG ===");
                return i; // Возвращаем первый неотвеченный вопрос
            }
        }

        // Если все вопросы отвечены, возвращаем первый
        System.out.println("All questions answered, returning first question");
        System.out.println("=== END DEBUG ===");
        return 0;
    }

    // Перейти к конкретному вопросу
    @Transactional
    public TestProgressResponse goToQuestion(Long attemptId, Long questionId, Long employeeId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        if (!attempt.getUser().getId().equals(employeeId)) {
            throw new RuntimeException("This attempt belongs to another user");
        }

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new RuntimeException("Attempt is not in progress");
        }

        // Находим вопрос
        Question question = questionRepository.findByIdWithOptions(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        if (!question.getTest().getId().equals(attempt.getTest().getId())) {
            throw new RuntimeException("Question does not belong to this test");
        }

        // Находим все вопросы теста
        List<Question> allQuestions = questionRepository.findByTestWithOptions(attempt.getTest());

        // Находим индекс текущего вопроса
        int questionIndex = -1;
        for (int i = 0; i < allQuestions.size(); i++) {
            if (allQuestions.get(i).getId().equals(questionId)) {
                questionIndex = i;
                break;
            }
        }

        if (questionIndex == -1) {
            throw new RuntimeException("Question not found in this test");
        }

        // Загружаем ответы
        List<Answer> answers = answerRepository.findByAttemptWithQuestions(attempt);

        // Создаем ответ
        TestProgressResponse response = new TestProgressResponse();
        response.setAttemptId(attemptId);
        response.setTestId(attempt.getTest().getId());
        response.setTestTitle(attempt.getTest().getTitle());
        response.setStartedAt(attempt.getStartedAt());
        response.setAutoSubmitAt(attempt.getAutoSubmitAt());
        response.setTimeLeftMinutes(calculateTimeLeft(attempt));
        response.setCurrentQuestionIndex(questionIndex);
        response.setTotalQuestions(allQuestions.size());

        // Создаем DTO вопроса
        QuestionWithAnswerResponse questionResponse = createQuestionWithAnswerResponse(question, answers);
        response.setCurrentQuestion(questionResponse);

        // Создаем прогресс вопросов
        List<QuestionProgressResponse> questionProgress = createQuestionProgress(allQuestions, answers);
        response.setQuestionProgress(questionProgress);

        return response;
    }

    private QuestionWithAnswerResponse createQuestionWithAnswerResponse(Question question, List<Answer> answers) {
        QuestionWithAnswerResponse response = new QuestionWithAnswerResponse();
        response.setId(question.getId());
        response.setText(question.getText());
        response.setType(question.getType().name());
        response.setOrderIndex(question.getOrderIndex());

        // Добавляем варианты ответов для CHOICE вопросов
        if (question.getType() == QuestionType.SINGLE_CHOICE ||
                question.getType() == QuestionType.MULTIPLE_CHOICE) {

            if (question.getOptions() != null) {
                List<AnswerOptionResponse> options = question.getOptions().stream()
                        .map(option -> {
                            AnswerOptionResponse optionResponse = new AnswerOptionResponse();
                            optionResponse.setId(option.getId());
                            optionResponse.setText(option.getText());
                            optionResponse.setOrderIndex(option.getOrderIndex());
                            optionResponse.setIsCorrect(false); // Не показываем пользователю
                            return optionResponse;
                        })
                        .collect(Collectors.toList());
                response.setOptions(options);
            }
        }

        // Ищем сохраненный ответ
        Optional<Answer> existingAnswer = answers.stream()
                .filter(a -> a.getQuestion() != null &&
                        a.getQuestion().getId().equals(question.getId()))
                .findFirst();

        if (existingAnswer.isPresent()) {
            Answer answer = existingAnswer.get();

            if (question.getType() == QuestionType.OPEN_ANSWER) {
                response.setPreviousAnswer(answer.getOpenAnswerText());
            } else {
                response.setPreviousSelectedOptions(answer.getSelectedOptionIds());
            }
        }

        return response;
    }

    private TestProgressResponse createProgressResponseWithSpecificQuestion(
            Attempt attempt, List<Question> questions, int questionIndex) {

        TestProgressResponse response = new TestProgressResponse();

        // Базовые поля
        response.setAttemptId(attempt.getId());
        response.setTestId(attempt.getTest().getId());
        response.setTestTitle(attempt.getTest().getTitle());
        response.setStartedAt(attempt.getStartedAt());
        response.setAutoSubmitAt(attempt.getAutoSubmitAt());
        response.setTimeLeftMinutes(calculateTimeLeft(attempt));

        response.setTotalQuestions(questions.size());
        response.setCurrentQuestionIndex(questionIndex);

        // Загружаем ответы
        List<Answer> answers = answerRepository.findByAttemptWithQuestions(attempt);

        // Создаем прогресс вопросов
        List<QuestionProgressResponse> questionProgress = createQuestionProgress(questions, answers);
        response.setQuestionProgress(questionProgress);

        // Устанавливаем текущий вопрос
        if (!questions.isEmpty() && questionIndex < questions.size()) {
            Question question = questions.get(questionIndex);
            QuestionWithAnswerResponse questionResponse = createSimpleQuestionResponse(question, attempt, answers);
            response.setCurrentQuestion(questionResponse);
        }

        return response;
    }

    // Получить все вопросы для попытки
    public List<QuestionResponse> getAllQuestionsForAttempt(Long attemptId, Long employeeId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        if (!attempt.getUser().getId().equals(employeeId)) {
            throw new RuntimeException("This attempt belongs to another user");
        }

        List<Question> questions = questionRepository.findByTestWithOptions(attempt.getTest());

        return questions.stream()
                .map(this::mapToQuestionResponse)
                .collect(Collectors.toList());
    }

    private QuestionResponse mapToQuestionResponse(Question question) {
        QuestionResponse response = new QuestionResponse();
        response.setId(question.getId());
        response.setText(question.getText());
        response.setType(question.getType().name());
        response.setMaxScore(question.getMaxScore());
        response.setOrderIndex(question.getOrderIndex());

        if (question.getOptions() != null) {
            List<AnswerOptionResponse> options = question.getOptions().stream()
                    .map(this::mapToAnswerOptionResponse)
                    .collect(Collectors.toList());
            response.setOptions(options);
        }

        return response;
    }

    private AnswerOptionResponse mapToAnswerOptionResponse(AnswerOption option) {
        AnswerOptionResponse response = new AnswerOptionResponse();
        response.setId(option.getId());
        response.setText(option.getText());
        response.setOrderIndex(option.getOrderIndex());
        response.setIsCorrect(false); // НЕ показываем правильность пользователю
        return response;
    }

    public List<QuestionWithAnswerResponse> getAllQuestionsForAttemptWithAnswers(Long attemptId, Long employeeId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        if (!attempt.getUser().getId().equals(employeeId)) {
            throw new RuntimeException("This attempt belongs to another user");
        }

        List<Question> questions = questionRepository.findByTestWithOptions(attempt.getTest());
        List<Answer> answers = answerRepository.findByAttemptWithQuestions(attempt);

        return questions.stream()
                .map(question -> createQuestionWithAnswerResponse(question, answers))
                .collect(Collectors.toList());
    }

    public QuestionWithAnswerResponse getQuestionWithAnswer(Long attemptId, Long questionId, Long employeeId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        if (!attempt.getUser().getId().equals(employeeId)) {
            throw new RuntimeException("This attempt belongs to another user");
        }

        Question question = questionRepository.findByIdWithOptions(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        if (!question.getTest().getId().equals(attempt.getTest().getId())) {
            throw new RuntimeException("Question does not belong to this test");
        }

        List<Answer> answers = answerRepository.findByAttemptWithQuestions(attempt);

        return createQuestionWithAnswerResponse(question, answers);
    }
}
