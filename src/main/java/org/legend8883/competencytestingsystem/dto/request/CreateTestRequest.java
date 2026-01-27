package org.legend8883.competencytestingsystem.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CreateTestRequest {

    @NotBlank(message = "Название теста обязательно")
    @Size(min = 3, max = 200, message = "Название должно быть от 3 до 200 символов")
    private String title;

    @Size(max = 1000, message = "Описание не должно превышать 1000 символов")
    private String description;

    @NotNull(message = "Время на прохождение обязательно")
    @Min(value = 5, message = "Минимальное время - 5 минут")
    @Max(value = 180, message = "Максимальное время - 180 минут")
    private Integer timeLimitMinutes;

    @NotNull(message = "Проходной балл обязателен")
    @Min(value = 0, message = "Проходной балл не может быть отрицательным")
    private Integer passingScore;

    private List<QuestionRequest> questions = new ArrayList<>();
}
