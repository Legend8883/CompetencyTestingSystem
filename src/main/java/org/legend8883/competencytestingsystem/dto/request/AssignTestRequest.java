package org.legend8883.competencytestingsystem.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AssignTestRequest {

    @NotNull(message = "Список пользователей обязателен")
    private List<Long> userIds;

    private LocalDateTime deadline;
}
