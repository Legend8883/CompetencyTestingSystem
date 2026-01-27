package org.legend8883.competencytestingsystem.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class QuestionResponse {
    private Long id;
    private String text;
    private String type;
    private Integer maxScore;
    private Integer orderIndex;
    private List<AnswerOptionResponse> options;
}
