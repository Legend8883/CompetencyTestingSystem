package org.legend8883.competencytestingsystem.dto.response;

import lombok.Data;

@Data
public class AnswerOptionWithSelectionResponse {
    private Long id;
    private String text;
    private Boolean isCorrect;
    private Integer orderIndex;
    private Boolean selected;  // <- Добавляем это поле
}
