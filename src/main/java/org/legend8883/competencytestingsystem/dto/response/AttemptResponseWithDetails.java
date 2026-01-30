package org.legend8883.competencytestingsystem.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AttemptResponseWithDetails extends AttemptResponse {
    private Boolean hasOpenQuestions;
    private Integer totalQuestions;
    private Integer answeredQuestions;
}
