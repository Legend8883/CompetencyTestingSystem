package org.legend8883.competencytestingsystem.mapper;

import org.legend8883.competencytestingsystem.dto.response.AttemptResponse;
import org.legend8883.competencytestingsystem.dto.response.TestProgressResponse;
import org.legend8883.competencytestingsystem.dto.response.TestResultResponse;
import org.legend8883.competencytestingsystem.entity.Attempt;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface AttemptMapper {

    // Entity → TestProgressResponse
    @Mapping(source = "id", target = "attemptId")
    @Mapping(source = "test.id", target = "testId")
    @Mapping(source = "test.title", target = "testTitle")
    @Mapping(target = "timeLeftMinutes", ignore = true)
    @Mapping(target = "currentQuestion", ignore = true)  // ← Будет установлен в сервисе
    @Mapping(target = "currentQuestionIndex", ignore = true)  // ← Будет установлен в сервисе
    @Mapping(target = "totalQuestions", ignore = true)  // ← Будет установлен в сервисе
    @Mapping(target = "questionProgress", ignore = true)  // ← Будет установлен в сервисе
    TestProgressResponse toProgressDto(Attempt attempt);

    // Entity → TestResultResponse (для детальных результатов)
    @Mapping(source = "id", target = "attemptId")
    @Mapping(source = "test.id", target = "testId")
    @Mapping(source = "test.title", target = "testTitle")
    @Mapping(source = "user", target = "user")
    @Mapping(source = "test.passingScore", target = "passingScore")
    @Mapping(target = "maxPossibleScore", ignore = true)
    @Mapping(target = "passed", expression = "java(isAttemptPassed(attempt))")
    @Mapping(target = "correctAnswersCount", ignore = true)
    @Mapping(target = "totalQuestions", ignore = true)
    @Mapping(target = "answers", ignore = true)
    TestResultResponse toResultDto(Attempt attempt);

    // Entity → AttemptResponse (для списка попыток)
    @Mapping(source = "test.id", target = "testId")
    @Mapping(source = "test.title", target = "testTitle")
    @Mapping(source = "user", target = "user")
    @Mapping(target = "passed", expression = "java(isAttemptPassed(attempt))")
    AttemptResponse toAttemptResponse(Attempt attempt);

    // Дефолтный метод для проверки прохождения
    default boolean isAttemptPassed(Attempt attempt) {
        if (attempt.getScore() == null || attempt.getTest() == null ||
                attempt.getTest().getPassingScore() == null) {
            return false;
        }
        return attempt.getScore() >= attempt.getTest().getPassingScore();
    }
}
