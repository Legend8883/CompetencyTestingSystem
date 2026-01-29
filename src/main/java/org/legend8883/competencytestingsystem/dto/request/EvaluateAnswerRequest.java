package org.legend8883.competencytestingsystem.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EvaluateAnswerRequest {

    @NotNull(message = "Балл обязателен")
    @Min(value = 0, message = "Балл не может быть отрицательным")
    private Integer score;
}
