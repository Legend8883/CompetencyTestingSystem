package org.legend8883.competencytestingsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AnswerOptionRequest {

    @NotBlank(message = "Текст варианта ответа обязателен")
    @Size(max = 500, message = "Текст варианта ответа не должен превышать 500 символов")
    private String text;

    private Boolean isCorrect = false;

    private Integer orderIndex;
}
