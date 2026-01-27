package org.legend8883.competencytestingsystem.service;

import lombok.RequiredArgsConstructor;
import org.legend8883.competencytestingsystem.dto.request.EvaluateAnswerRequest;
import org.legend8883.competencytestingsystem.entity.*;
import org.legend8883.competencytestingsystem.repository.AnswerRepository;
import org.legend8883.competencytestingsystem.repository.AttemptRepository;
import org.legend8883.competencytestingsystem.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final AnswerRepository answerRepository;
    private final AttemptRepository attemptRepository;
    private final UserRepository userRepository;

    // Получить открытые вопросы для проверки
    public List<Answer> getOpenAnswersForEvaluation(Long hrId) {
        // Проверить что пользователь HR
        User hr = userRepository.findById(hrId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (hr.getRole() != Role.HR) {
            throw new RuntimeException("Only HR can evaluate answers");
        }

        return answerRepository.findOpenAnswersForEvaluation();
    }

    // Оценить открытый вопрос
    @Transactional
    public Answer evaluateAnswer(Long answerId, EvaluateAnswerRequest request, Long hrId) {
        Answer answer = answerRepository.findByIdWithQuestion(answerId)
                .orElseThrow(() -> new RuntimeException("Answer not found with question"));

        // Проверить что это открытый вопрос
        if (answer.getQuestion().getType() != QuestionType.OPEN_ANSWER) {
            throw new RuntimeException("Only open answers can be evaluated manually");
        }

        // Проверить максимальный балл вопроса
        if (request.getScore() > answer.getQuestion().getMaxScore()) {
            throw new RuntimeException("Score cannot exceed maximum score: " +
                    answer.getQuestion().getMaxScore());
        }

        if (request.getScore() < 0) {
            throw new RuntimeException("Score cannot be negative");
        }

        // Установить оценку
        answer.setAssignedScore(request.getScore());
        Answer savedAnswer = answerRepository.save(answer);

        // Пересчитать общий балл попытки
        recalculateAttemptScore(answer.getAttempt().getId());

        return savedAnswer;
    }

    // Получить попытки для проверки
    public List<Attempt> getAttemptsForEvaluation(Long hrId) {
        return attemptRepository.findByStatus(AttemptStatus.EVALUATING);
    }

    // Завершить проверку попытки
    @Transactional
    public Attempt completeEvaluation(Long attemptId, Long hrId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        // Загружаем ответы для этой попытки
        List<Answer> answers = answerRepository.findByAttempt(attempt);

        // Проверить что все открытые вопросы оценены
        boolean allEvaluated = answers.stream()
                .filter(answer -> {
                    // Нужно загрузить вопрос для каждого ответа
                    Answer fullAnswer = answerRepository.findByIdWithQuestion(answer.getId())
                            .orElse(answer);
                    return fullAnswer.getQuestion().getType() == QuestionType.OPEN_ANSWER;
                })
                .allMatch(answer -> answer.getAssignedScore() != null);

        if (!allEvaluated) {
            throw new RuntimeException("Not all open answers have been evaluated");
        }

        attempt.setStatus(AttemptStatus.EVALUATED);
        return attemptRepository.save(attempt);
    }

    private void recalculateAttemptScore(Long attemptId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

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
        attemptRepository.save(attempt);
    }
}
