package org.legend8883.competencytestingsystem.service;

import lombok.RequiredArgsConstructor;
import org.legend8883.competencytestingsystem.dto.response.UserSimpleResponse;
import org.legend8883.competencytestingsystem.entity.Role;
import org.legend8883.competencytestingsystem.entity.User;
import org.legend8883.competencytestingsystem.mapper.UserMapper;
import org.legend8883.competencytestingsystem.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    // Получить всех пользователей
    public List<UserSimpleResponse> getAllUsers() {
        return userMapper.toDtoList(userRepository.findAll());
    }

    // Получить пользователей по роли
    public List<UserSimpleResponse> getUsersByRole(Role role) {
        return userMapper.toDtoList(userRepository.findByRole(role));
    }

    // Получить всех сотрудников (для назначения тестов)
    public List<UserSimpleResponse> getAllEmployees() {
        return getUsersByRole(Role.EMPLOYEE);
    }

    // Получить всех HR
    public List<UserSimpleResponse> getAllHR() {
        return getUsersByRole(Role.HR);
    }

    // Получить пользователя по ID
    public UserSimpleResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return userMapper.toDto(user);
    }

    // Поиск пользователей по имени/фамилии
    public List<UserSimpleResponse> searchUsers(String query) {
        List<User> users = userRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(query, query);
        return userMapper.toDtoList(users);
    }
}
