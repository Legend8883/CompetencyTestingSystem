package org.legend8883.competencytestingsystem.service;

import lombok.RequiredArgsConstructor;
import org.legend8883.competencytestingsystem.dto.request.CreateTestRequest;
import org.legend8883.competencytestingsystem.dto.response.TestResponse;
import org.legend8883.competencytestingsystem.entity.*;
import org.legend8883.competencytestingsystem.mapper.QuestionMapper;
import org.legend8883.competencytestingsystem.mapper.TestMapper;
import org.legend8883.competencytestingsystem.repository.AnswerOptionRepository;
import org.legend8883.competencytestingsystem.repository.QuestionRepository;
import org.legend8883.competencytestingsystem.repository.TestRepository;
import org.legend8883.competencytestingsystem.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TestService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final UserRepository userRepository;
    private final TestMapper testMapper;
    private final QuestionMapper questionMapper;

    // Создать новый тест
    @Transactional
    public TestResponse createTest(CreateTestRequest request, Long hrId) {
        // 1. Найти HR пользователя
        User hr = userRepository.findById(hrId)
                .orElseThrow(() -> new RuntimeException("HR not found with id: " + hrId));

        // 2. Проверить что HR имеет правильную роль
        if (hr.getRole() != Role.HR) {
            throw new RuntimeException("Only HR can create tests");
        }

        // 3. Создать тест
        Test test = testMapper.toEntity(request);
        test.setCreatedBy(hr);

        // 4. Сохранить тест
        Test savedTest = testRepository.save(test);

        // 5. Создать вопросы и варианты ответов
        if (request.getQuestions() != null) {
            List<Question> questions = new ArrayList<>();

            for (int i = 0; i < request.getQuestions().size(); i++) {
                var questionRequest = request.getQuestions().get(i);

                // Создать вопрос
                Question question = questionMapper.toEntity(questionRequest);
                question.setTest(savedTest);
                question.setOrderIndex(i);

                // Определить тип вопроса
                question.setType(QuestionType.valueOf(questionRequest.getType()));

                // Сохранить вопрос
                Question savedQuestion = questionRepository.save(question);

                // Создать варианты ответов (только для CHOICE вопросов)
                if (questionRequest.getOptions() != null &&
                        (question.getType() == QuestionType.SINGLE_CHOICE ||
                                question.getType() == QuestionType.MULTIPLE_CHOICE)) {

                    List<AnswerOption> options = new ArrayList<>();

                    for (int j = 0; j < questionRequest.getOptions().size(); j++) {
                        var optionRequest = questionRequest.getOptions().get(j);

                        AnswerOption option = new AnswerOption();
                        option.setText(optionRequest.getText());

                        // ИСПРАВЛЕНИЕ: гарантируем что isCorrect не null
                        option.setIsCorrect(optionRequest.getIsCorrect() != null ?
                                optionRequest.getIsCorrect() : false);

                        option.setQuestion(savedQuestion);
                        option.setOrderIndex(j);

                        options.add(option);
                        System.out.println("DEBUG: Created option - Text: " + option.getText() +
                                ", isCorrect: " + option.getIsCorrect());
                    }

                    answerOptionRepository.saveAll(options);
                    savedQuestion.setOptions(options);
                }

                questions.add(savedQuestion);
            }

            savedTest.setQuestions(questions);
        }

        // 6. Вернуть DTO
        return testMapper.toDto(savedTest);
    }

    // Получить все тесты HR
    public List<TestResponse> getTestsByHr(Long hrId) {
        User hr = userRepository.findById(hrId)
                .orElseThrow(() -> new RuntimeException("HR not found"));

        List<Test> tests = testRepository.findByCreatedBy(hr);
        return tests.stream()
                .map(testMapper::toDto)
                .toList();
    }

    // Получить тест по ID
    public TestResponse getTestById(Long testId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found with id: " + testId));
        return testMapper.toDto(test);
    }

    // Получить тест с вопросами для прохождения
    public TestResponse getTestWithQuestions(Long testId) {
        Test test = testRepository.findByIdWithQuestions(testId)
                .orElseThrow(() -> new RuntimeException("Test not found with id: " + testId));
        return testMapper.toDto(test);
    }

    // Активировать/деактивировать тест
    @Transactional
    public TestResponse toggleTestStatus(Long testId, Long hrId, boolean isActive) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found"));

        // Проверить что тест принадлежит этому HR
        if (!test.getCreatedBy().getId().equals(hrId)) {
            throw new RuntimeException("You can only modify your own tests");
        }

        test.setIsActive(isActive);
        Test updatedTest = testRepository.save(test);

        return testMapper.toDto(updatedTest);
    }
}
