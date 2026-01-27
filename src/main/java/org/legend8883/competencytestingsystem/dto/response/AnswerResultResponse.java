package org.legend8883.competencytestingsystem.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class AnswerResultResponse {
    private Long questionId;
    private String questionText;
    private String questionType;
    private Integer questionMaxScore;
    private String userAnswer;
    private List<AnswerOptionResponse> selectedOptions;
    private Integer assignedScore;
    private Integer autoScore;
    private Integer finalScore;
    private Boolean isCorrect;
}
