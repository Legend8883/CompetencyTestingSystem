package org.legend8883.competencytestingsystem.mapper;

import org.legend8883.competencytestingsystem.dto.response.AnswerResponse;
import org.legend8883.competencytestingsystem.entity.Answer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AnswerMapper {

    @Mapping(source = "question.id", target = "questionId")
    @Mapping(source = "question.text", target = "questionText")
    @Mapping(source = "question.type", target = "questionType")
    @Mapping(source = "question.maxScore", target = "maxScore")
    AnswerResponse toDto(Answer answer);
}