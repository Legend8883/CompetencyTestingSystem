package org.legend8883.competencytestingsystem.service;

import lombok.RequiredArgsConstructor;
import org.legend8883.competencytestingsystem.dto.request.StartTestRequest;
import org.legend8883.competencytestingsystem.dto.request.SubmitAnswerRequest;
import org.legend8883.competencytestingsystem.dto.response.AnswerOptionResponse;
import org.legend8883.competencytestingsystem.dto.response.QuestionProgressResponse;
import org.legend8883.competencytestingsystem.dto.response.QuestionWithAnswerResponse;
import org.legend8883.competencytestingsystem.dto.response.TestProgressResponse;
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

        // Обновить статус
        boolean hasOpenQuestions = hasOpenQuestions(attempt);
        attempt.setStatus(hasOpenQuestions ? AttemptStatus.EVALUATING : AttemptStatus.COMPLETED);
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

        return createProgressResponse(attempt);
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

            // Создаем прогресс вопросов
            List<Answer> answers = answerRepository.findByAttemptWithQuestions(attempt);
            System.out.println("Answers loaded: " + answers.size());

            List<QuestionProgressResponse> questionProgress = createQuestionProgress(questions, answers);
            response.setQuestionProgress(questionProgress);

            // Находим текущий вопрос
            if (!questions.isEmpty()) {
                Question question = questions.get(0); // Всегда первый вопрос
                response.setCurrentQuestionIndex(0);

                QuestionWithAnswerResponse questionResponse = createSimpleQuestionResponse(question, attempt);
                response.setCurrentQuestion(questionResponse);

                System.out.println("Current question set: ID=" + question.getId());
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

        // КРИТИЧЕСКОЕ ИСПРАВЛЕНИЕ: если currentQuestion все еще null, устанавливаем его
        if (response.getCurrentQuestion() == null && response.getTotalQuestions() > 0) {
            try {
                // Получаем вопросы еще раз
                List<Question> questions = questionRepository.findByTestWithOptions(attempt.getTest());
                if (!questions.isEmpty()) {
                    Question question = questions.get(0);
                    response.setCurrentQuestionIndex(0);
                    QuestionWithAnswerResponse questionResponse = createSimpleQuestionResponse(question, attempt);
                    response.setCurrentQuestion(questionResponse);
                    System.out.println("FORCE SET currentQuestion to first question");
                }
            } catch (Exception e) {
                System.err.println("Failed to force set currentQuestion: " + e.getMessage());
            }
        }

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
}
