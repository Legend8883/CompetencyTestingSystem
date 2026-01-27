package org.legend8883.competencytestingsystem.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class SubmitAnswerRequest {

    private Long questionId;

    // Для вопросов с выбором
    private List<Long> selectedOptionIds;

    // Для открытых вопросов
    private String openAnswerText;
}