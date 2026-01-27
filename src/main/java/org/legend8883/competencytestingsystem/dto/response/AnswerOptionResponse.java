package org.legend8883.competencytestingsystem.dto.response;

import lombok.Data;

@Data
public class AnswerOptionResponse {
    private Long id;
    private String text;
    private Boolean isCorrect;
    private Integer orderIndex;
}
