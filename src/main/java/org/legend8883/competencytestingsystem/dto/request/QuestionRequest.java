package org.legend8883.competencytestingsystem.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class QuestionRequest {

    @NotBlank(message = "Текст вопроса обязателен")
    @Size(min = 5, max = 1000, message = "Текст вопроса должен быть от 5 до 1000 символов")
    private String text;

    @NotNull(message = "Тип вопроса обязателен")
    private String type; // "SINGLE_CHOICE", "MULTIPLE_CHOICE", "OPEN_ANSWER"

    @NotNull(message = "Максимальный балл обязателен")
    @Min(value = 1, message = "Минимальный балл - 1")
    @Max(value = 100, message = "Максимальный балл - 100")
    private Integer maxScore;

    private List<AnswerOptionRequest> options = new ArrayList<>();

    // Только для OPEN_ANSWER вопросов
    private String correctOpenAnswer;

    private Integer orderIndex;
}
