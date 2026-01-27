package org.legend8883.competencytestingsystem.dto.response;

import lombok.Data;

@Data
public class AnswerResponse {
    private Long id;
    private Long questionId;
    private String questionText;
    private String questionType;
    private String openAnswerText;
    private Integer assignedScore;
    private Integer autoScore;
    private Integer maxScore;
}
