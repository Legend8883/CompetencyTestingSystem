package org.legend8883.competencytestingsystem.mapper;

import org.legend8883.competencytestingsystem.dto.request.CreateTestRequest;
import org.legend8883.competencytestingsystem.dto.response.TestResponse;
import org.legend8883.competencytestingsystem.entity.Test;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = {QuestionMapper.class, UserMapper.class})
public interface TestMapper extends BaseMapper<Test, TestResponse> {

    // Entity → DTO
    @Override
    @Mapping(source = "questions", target = "questionCount", qualifiedByName = "countQuestions")
    @Mapping(target = "maxPossibleScore", expression = "java(calculateMaxPossibleScore(test))")
    @Mapping(source = "createdBy", target = "createdBy")
    TestResponse toDto(Test test);

    // CreateTestRequest → Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "questions", ignore = true)
    Test toEntity(CreateTestRequest request);

    // DTO → Entity (из BaseMapper - нужно переопределить)
    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "questions", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Test toEntity(TestResponse dto);

    // Метод для подсчета вопросов
    @Named("countQuestions")
    default Integer countQuestions(List<?> questions) {
        return questions != null ? questions.size() : 0;
    }

    // Метод для вычисления максимального балла
    default Integer calculateMaxPossibleScore(Test test) {
        if (test.getQuestions() == null) return 0;
        return test.getQuestions().stream()
                .mapToInt(q -> q.getMaxScore() != null ? q.getMaxScore() : 0)
                .sum();
    }

    // Обновление теста из DTO
    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "questions", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(TestResponse dto, @MappingTarget Test entity);
}
