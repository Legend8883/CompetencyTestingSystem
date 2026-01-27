package org.legend8883.competencytestingsystem.mapper;

import org.legend8883.competencytestingsystem.dto.request.QuestionRequest;
import org.legend8883.competencytestingsystem.dto.response.QuestionResponse;
import org.legend8883.competencytestingsystem.dto.response.QuestionWithAnswerResponse;
import org.legend8883.competencytestingsystem.entity.Question;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = {AnswerOptionMapper.class})
public interface QuestionMapper {

    // Entity → QuestionResponse
    @Mapping(target = "options", source = "options")
    QuestionResponse toDto(Question question);

    // QuestionRequest → Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "test", ignore = true)
    @Mapping(target = "options", ignore = true)
    @Mapping(target = "correctOpenAnswer", source = "correctOpenAnswer")
    @Mapping(target = "correctOptions", ignore = true) // Добавляем!
    Question toEntity(QuestionRequest request);

    // Entity → QuestionWithAnswerResponse
    @Mapping(target = "previousAnswer", ignore = true)
    @Mapping(target = "previousSelectedOptions", ignore = true)
    QuestionWithAnswerResponse toQuestionWithAnswerDto(Question question);

    // Список вопросов
    List<QuestionResponse> toDtoList(List<Question> questions);

    // Обновление вопроса
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "test", ignore = true)
    @Mapping(target = "correctOptions", ignore = true)
    void updateEntityFromDto(QuestionRequest dto, @MappingTarget Question entity);
}
