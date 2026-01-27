package org.legend8883.competencytestingsystem.mapper;

import org.legend8883.competencytestingsystem.dto.request.AnswerOptionRequest;
import org.legend8883.competencytestingsystem.dto.response.AnswerOptionResponse;
import org.legend8883.competencytestingsystem.entity.AnswerOption;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AnswerOptionMapper {

    // Entity → DTO
    AnswerOptionResponse toDto(AnswerOption answerOption);

    // DTO → Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "question", ignore = true)
    AnswerOption toEntity(AnswerOptionRequest request);

    // Списки
    List<AnswerOptionResponse> toDtoList(List<AnswerOption> answerOptions);
    List<AnswerOption> toEntityList(List<AnswerOptionRequest> requests);
}
