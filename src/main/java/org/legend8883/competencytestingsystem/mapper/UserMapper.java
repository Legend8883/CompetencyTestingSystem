package org.legend8883.competencytestingsystem.mapper;

import org.legend8883.competencytestingsystem.dto.request.RegisterRequest;
import org.legend8883.competencytestingsystem.dto.response.UserSimpleResponse;
import org.legend8883.competencytestingsystem.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // Entity → DTO
    UserSimpleResponse toDto(User user);

    // DTO → Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(UserSimpleResponse dto);

    // RegisterRequest → User
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", constant = "EMPLOYEE")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(RegisterRequest registerRequest);

    // Список пользователей
    List<UserSimpleResponse> toDtoList(List<User> users);

    // Обновление пользователя (опционально)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(UserSimpleResponse dto, @MappingTarget User entity);
}