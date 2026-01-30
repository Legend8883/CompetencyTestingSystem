package org.legend8883.competencytestingsystem.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class AnswerWithDetailsResponse {
    private Long questionId;
    private String questionText;
    private String questionType;
    private Integer maxScore;
    private String openAnswerText;
    private Integer assignedScore;
    private Integer autoScore;
    private List<AnswerOptionSimpleResponse> selectedOptions;
}
