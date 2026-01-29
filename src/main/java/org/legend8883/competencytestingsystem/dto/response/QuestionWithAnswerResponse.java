package org.legend8883.competencytestingsystem.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class QuestionWithAnswerResponse {
    private Long id;
    private String text;
    private String type; // "SINGLE_CHOICE", "MULTIPLE_CHOICE", "OPEN_ANSWER"
    private Integer orderIndex;
    private List<AnswerOptionResponse> options; // Только для CHOICE вопросов
    private String previousAnswer; // Для открытых вопросов
    private List<Long> previousSelectedOptions; // Для вопросов с выбором
}
