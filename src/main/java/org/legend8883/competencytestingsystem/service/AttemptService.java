package org.legend8883.competencytestingsystem.service;

import lombok.RequiredArgsConstructor;
import org.legend8883.competencytestingsystem.dto.request.StartTestRequest;
import org.legend8883.competencytestingsystem.dto.request.SubmitAnswerRequest;
import org.legend8883.competencytestingsystem.dto.response.AnswerOptionResponse;
import org.legend8883.competencytestingsystem.dto.response.QuestionProgressResponse;
import org.legend8883.competencytestingsystem.dto.response.QuestionWithAnswerResponse;
import org.legend8883.competencytestingsystem.dto.response.TestProgressResponse;
import org.legend8883.competencytestingsystem.entity.*;
import org.legend8883.competencytestingsystem.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttemptService {

    private final AttemptRepository attemptRepository;
    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final TestAssignmentRepository testAssignmentRepository;
    private final UserRepository userRepository;

    // Начать тестирование
    @Transactional
    public TestProgressResponse startTest(StartTestRequest request, Long employeeId) {
        System.out.println("=== DEBUG startTest ===");
        System.out.println("Test ID: " + request.getTestId());
        System.out.println("Employee ID: " + employeeId);

        // 1. Найти сотрудника
        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));
        System.out.println("Employee found: " + employee.getEmail());

        // 2. Найти тест
        Test test = testRepository.findById(request.getTestId())
                .orElseThrow(() -> new RuntimeException("Test not found with id: " + request.getTestId()));
        System.out.println("Test found: " + test.getTitle());

        // 3. Проверить назначение
        Optional<TestAssignment> assignment = testAssignmentRepository
                .findByUserAndTest(employee, test);

        if (assignment.isEmpty()) {
            System.out.println("WARNING: Test is not assigned to employee");
            // Если хотите разрешить тестирование без назначения, раскомментируйте:
            // throw new RuntimeException("Test is not assigned to you");
        } else {
            System.out.println("Assignment found: " + assignment.get().getId());

            if (!assignment.get().getIsActive()) {
                throw new RuntimeException("Test assignment is not active");
            }

            if (assignment.get().getDeadline() != null &&
                    assignment.get().getDeadline().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Test deadline has passed");
            }
        }

        // 4. Проверить нет ли активной попытки
        Optional<Attempt> activeAttempt = attemptRepository
                .findByUserAndStatus(employee, AttemptStatus.IN_PROGRESS)
                .stream()
                .filter(a -> a.getTest().getId().equals(test.getId()))
                .findFirst();

        if (activeAttempt.isPresent()) {
            System.out.println("Active attempt found: " + activeAttempt.get().getId());
            // Возвращаем существующую попытку
            return createProgressResponse(activeAttempt.get());
        }

        // 5. Создать новую попытку
        Attempt attempt = new Attempt();
        attempt.setUser(employee);
        attempt.setTest(test);
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt.setScore(0);

        System.out.println("Creating new attempt...");
        Attempt savedAttempt = attemptRepository.save(attempt);
        System.out.println("Attempt created with ID: " + savedAttempt.getId());

        // 6. Создать пустые ответы для всех вопросов
        List<Question> questions = questionRepository.findByTestWithOptions(test);
        System.out.println("Test has " + questions.size() + " questions");

        if (!questions.isEmpty()) {
            List<Answer> answers = new ArrayList<>();

            for (Question question : questions) {
                Answer answer = new Answer();
                answer.setAttempt(savedAttempt);
                answer.setQuestion(question);
                answers.add(answer);
                System.out.println("Created answer for question: " + question.getId());
            }

            answerRepository.saveAll(answers);
            System.out.println("Answers saved: " + answers.size());
        }

        TestProgressResponse response = createProgressResponse(savedAttempt);
        System.out.println("Response created with attemptId: " + response.getAttemptId());
        System.out.println("=== END DEBUG startTest ===");

        return response;
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

        // Проверить есть ли открытые вопросы
        boolean hasOpenQuestions = hasOpenQuestions(attempt);

        if (hasOpenQuestions) {
            attempt.setStatus(AttemptStatus.EVALUATING);
        } else {
            // Нет открытых вопросов - сразу завершаем
            attempt.setStatus(AttemptStatus.EVALUATED);
        }

        attempt.setCompletedAt(LocalDateTime.now());

        attemptRepository.save(attempt);

        return createProgressResponse(attempt);
    }

    // Получить прогресс теста
    public TestProgressResponse getTestProgress(Long attemptId, Long employeeId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        if (!attempt.getUser().getId().equals(employeeId)) {
            throw new RuntimeException("This attempt belongs to another user");
        }

        return createFullProgressResponse(attempt);
    }




    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ

    private TestProgressResponse createFullProgressResponse(Attempt attempt) {
        TestProgressResponse response = new TestProgressResponse();

        // Базовые поля
        response.setAttemptId(attempt.getId());
        response.setTestId(attempt.getTest().getId());
        response.setTestTitle(attempt.getTest().getTitle());
        response.setStartedAt(attempt.getStartedAt());
        response.setAutoSubmitAt(attempt.getAutoSubmitAt());
        response.setTimeLeftMinutes(calculateTimeLeft(attempt));

        // Получаем ВСЕ вопросы теста
        List<Question> allQuestions = questionRepository.findByTestWithOptions(attempt.getTest());
        response.setTotalQuestions(allQuestions.size());

        // Получаем ВСЕ ответы
        List<Answer> allAnswers = answerRepository.findByAttemptWithQuestions(attempt);

        // Создаем прогресс для всех вопросов
        List<QuestionProgressResponse> questionProgress = createQuestionProgress(allQuestions, allAnswers);
        response.setQuestionProgress(questionProgress);

        // Находим первый неотвеченный вопрос
        Question currentQuestion = null;
        int currentIndex = 0;

        for (int i = 0; i < allQuestions.size(); i++) {
            Question question = allQuestions.get(i);
            boolean answered = allAnswers.stream()
                    .anyMatch(answer ->
                            answer.getQuestion().getId().equals(question.getId()) &&
                                    answer.getAnsweredAt() != null);

            if (!answered) {
                currentQuestion = question;
                currentIndex = i;
                break;
            }
        }

        // Если все отвечены - берем последний
        if (currentQuestion == null && !allQuestions.isEmpty()) {
            currentQuestion = allQuestions.get(allQuestions.size() - 1);
            currentIndex = allQuestions.size() - 1;
        }

        response.setCurrentQuestionIndex(currentIndex);

        // Создаем DTO для текущего вопроса
        if (currentQuestion != null) {
            QuestionWithAnswerResponse questionResponse = createQuestionWithAnswerResponse(currentQuestion, allAnswers);
            response.setCurrentQuestion(questionResponse);
        }

        return response;
    }

    private QuestionWithAnswerResponse createQuestionWithAnswerResponse(Question question, List<Answer> allAnswers) {
        QuestionWithAnswerResponse response = new QuestionWithAnswerResponse();
        response.setId(question.getId());
        response.setText(question.getText());
        response.setType(question.getType().name());
        response.setOrderIndex(question.getOrderIndex());

        // Добавляем варианты ответов
        if (question.getOptions() != null && !question.getOptions().isEmpty()) {
            List<AnswerOptionResponse> options = new ArrayList<>();
            for (AnswerOption option : question.getOptions()) {
                AnswerOptionResponse optionResponse = new AnswerOptionResponse();
                optionResponse.setId(option.getId());
                optionResponse.setText(option.getText());
                optionResponse.setOrderIndex(option.getOrderIndex());
                optionResponse.setIsCorrect(false); // Не показываем правильность во время теста
                options.add(optionResponse);
            }
            response.setOptions(options);
        }

        // Находим предыдущий ответ если есть
        Optional<Answer> previousAnswer = allAnswers.stream()
                .filter(a -> a.getQuestion().getId().equals(question.getId()))
                .findFirst();

        if (previousAnswer.isPresent()) {
            Answer answer = previousAnswer.get();

            if (question.getType() == QuestionType.OPEN_ANSWER) {
                response.setPreviousAnswer(answer.getOpenAnswerText());
            } else if (question.getType() == QuestionType.SINGLE_CHOICE ||
                    question.getType() == QuestionType.MULTIPLE_CHOICE) {
                response.setPreviousSelectedOptions(answer.getSelectedOptionIds());
            }
        }

        return response;
    }

    private TestProgressResponse createProgressResponse(Attempt attempt) {
        System.out.println("=== DEBUG createProgressResponse ===");
        System.out.println("Attempt ID: " + attempt.getId());
        System.out.println("Test ID: " + (attempt.getTest() != null ? attempt.getTest().getId() : "null"));
        System.out.println("User ID: " + (attempt.getUser() != null ? attempt.getUser().getId() : "null"));

        TestProgressResponse response = new TestProgressResponse();

        try {
            // Базовые поля
            response.setAttemptId(attempt.getId());
            response.setTestId(attempt.getTest().getId());
            response.setTestTitle(attempt.getTest().getTitle());
            response.setStartedAt(attempt.getStartedAt());
            response.setAutoSubmitAt(attempt.getAutoSubmitAt());

            // Время
            Integer timeLeft = calculateTimeLeft(attempt);
            response.setTimeLeftMinutes(timeLeft);
            System.out.println("Time left: " + timeLeft + " minutes");

            // Получаем вопросы теста
            List<Question> questions = questionRepository.findByTestWithOptions(attempt.getTest());
            response.setTotalQuestions(questions.size());
            System.out.println("Total questions: " + questions.size());

            // Получаем ответы
            List<Answer> answers = answerRepository.findByAttemptWithQuestions(attempt);
            System.out.println("Answers found: " + answers.size());

            // Создаем прогресс вопросов
            List<QuestionProgressResponse> questionProgress = createQuestionProgress(questions, answers);
            response.setQuestionProgress(questionProgress);

            // НАХОДИМ ТЕКУЩИЙ ВОПРОС
            Question currentQuestion = null;
            int currentIndex = 0;

            for (int i = 0; i < questions.size(); i++) {
                Question question = questions.get(i);
                boolean answered = answers.stream()
                        .anyMatch(answer ->
                                answer.getQuestion().getId().equals(question.getId()) &&
                                        answer.getAnsweredAt() != null);

                if (!answered) {
                    currentQuestion = question;
                    currentIndex = i;
                    break;
                }
            }

            // Если все вопросы отвечены, берем последний
            if (currentQuestion == null && !questions.isEmpty()) {
                currentQuestion = questions.get(questions.size() - 1);
                currentIndex = questions.size() - 1;
            }

            response.setCurrentQuestionIndex(currentIndex);
            System.out.println("Current question index: " + currentIndex);

            // Создаем DTO для текущего вопроса
            if (currentQuestion != null) {
                System.out.println("Current question ID: " + currentQuestion.getId());
                QuestionWithAnswerResponse questionResponse = createSimpleQuestionResponse(currentQuestion, attempt);
                response.setCurrentQuestion(questionResponse);
            }

        } catch (Exception e) {
            System.err.println("ERROR in createProgressResponse: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("=== END DEBUG createProgressResponse ===");
        return response;
    }

    private QuestionWithAnswerResponse createSimpleQuestionResponse(Question question, Attempt attempt) {
        System.out.println("=== DEBUG createSimpleQuestionResponse ===");
        System.out.println("Question ID: " + question.getId());
        System.out.println("Question Type: " + question.getType());
        System.out.println("Question has options field? " + (question.getOptions() != null));

        if (question.getOptions() != null) {
            System.out.println("Options list size: " + question.getOptions().size());

            // Проверим каждый вариант БЕЗ вызова toString() для всего объекта
            for (int i = 0; i < question.getOptions().size(); i++) {
                AnswerOption option = question.getOptions().get(i);
                System.out.println("  Option[" + i + "]: ID=" + option.getId() +
                        ", Text length=" + (option.getText() != null ? option.getText().length() : "null") +
                        ", IsCorrect=" + option.getIsCorrect());
            }
        } else {
            System.out.println("WARNING: question.getOptions() is NULL!");
        }

        QuestionWithAnswerResponse response = new QuestionWithAnswerResponse();
        response.setId(question.getId());
        response.setText(question.getText());
        response.setType(question.getType().name());
        response.setOrderIndex(question.getOrderIndex());

        // Добавляем варианты ответов
        if (question.getOptions() != null && !question.getOptions().isEmpty()) {
            List<AnswerOptionResponse> options = new ArrayList<>();
            for (AnswerOption option : question.getOptions()) {
                AnswerOptionResponse optionResponse = new AnswerOptionResponse();
                optionResponse.setId(option.getId());
                optionResponse.setText(option.getText());
                optionResponse.setOrderIndex(option.getOrderIndex());
                optionResponse.setIsCorrect(false); // Не показываем фронтенду!
                options.add(optionResponse);
                System.out.println("Added option to DTO: ID=" + option.getId() + ", Text length=" + option.getText().length());
            }
            response.setOptions(options);
            System.out.println("Total options in DTO: " + options.size());
        } else {
            System.out.println("ERROR: No options to add to DTO!");
        }

        System.out.println("Final DTO options: " + (response.getOptions() != null ? response.getOptions().size() : "null"));
        System.out.println("=== END DEBUG createSimpleQuestionResponse ===");

        return response;
    }

    private Integer calculateTimeLeft(Attempt attempt) {
        if (attempt.getAutoSubmitAt() == null) {
            System.out.println("WARNING: autoSubmitAt is null!");
            return 0;
        }

        if (attempt.getCompletedAt() != null) {
            System.out.println("Test already completed at: " + attempt.getCompletedAt());
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        System.out.println("DEBUG: now = " + now + ", autoSubmitAt = " + attempt.getAutoSubmitAt());

        if (now.isAfter(attempt.getAutoSubmitAt())) {
            System.out.println("WARNING: Time's up! now is after autoSubmitAt");
            return 0;
        }

        long secondsLeft = java.time.Duration.between(now, attempt.getAutoSubmitAt()).getSeconds();
        int minutesLeft = (int) Math.max(0, secondsLeft / 60);

        System.out.println("DEBUG: secondsLeft = " + secondsLeft + ", minutesLeft = " + minutesLeft);

        return minutesLeft;
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

    public TestProgressResponse getQuestion(Long attemptId, Long questionId, Long employeeId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        if (!attempt.getUser().getId().equals(employeeId)) {
            throw new RuntimeException("This attempt belongs to another user");
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        // Создаем базовый response
        TestProgressResponse response = new TestProgressResponse();
        response.setAttemptId(attempt.getId());
        response.setTestId(attempt.getTest().getId());
        response.setTestTitle(attempt.getTest().getTitle());
        response.setStartedAt(attempt.getStartedAt());
        response.setAutoSubmitAt(attempt.getAutoSubmitAt());
        response.setTimeLeftMinutes(calculateTimeLeft(attempt));

        // Получаем все вопросы
        List<Question> questions = questionRepository.findByTestWithOptions(attempt.getTest());
        response.setTotalQuestions(questions.size());

        // Находим индекс текущего вопроса
        int currentIndex = -1;
        for (int i = 0; i < questions.size(); i++) {
            if (questions.get(i).getId().equals(questionId)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex == -1) {
            throw new RuntimeException("Question not found in this test");
        }

        response.setCurrentQuestionIndex(currentIndex);

        // Создаем вопрос с ответом
        QuestionWithAnswerResponse questionResponse = createSimpleQuestionResponse(question, attempt);
        response.setCurrentQuestion(questionResponse);

        // Создаем прогресс
        List<Answer> answers = answerRepository.findByAttemptWithQuestions(attempt);
        List<QuestionProgressResponse> questionProgress = createQuestionProgress(questions, answers);
        response.setQuestionProgress(questionProgress);

        return response;
    }

    public List<TestProgressResponse> getMyAttempts(Long employeeId) {
        User employee = new User();
        employee.setId(employeeId);

        List<Attempt> attempts = attemptRepository.findByUser(employee);

        return attempts.stream()
                .sorted((a1, a2) -> {
                    if (a1.getCompletedAt() == null && a2.getCompletedAt() == null) return 0;
                    if (a1.getCompletedAt() == null) return 1;
                    if (a2.getCompletedAt() == null) return -1;
                    return a2.getCompletedAt().compareTo(a1.getCompletedAt());
                })
                .map(this::createProgressResponse)
                .toList();
    }

    public TestProgressResponse goToQuestion(Long attemptId, Long questionId, Long employeeId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        if (!attempt.getUser().getId().equals(employeeId)) {
            throw new RuntimeException("This attempt belongs to another user");
        }

        return getQuestion(attemptId, questionId, employeeId);
    }
}
