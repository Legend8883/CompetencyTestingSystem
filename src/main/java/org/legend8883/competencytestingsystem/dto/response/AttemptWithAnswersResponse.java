package org.legend8883.competencytestingsystem.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AttemptWithAnswersResponse {
    private Long id;
    private Long testId;
    private String testTitle;
    private String status;
    private LocalDateTime completedAt;
    private Integer score;
    private Integer autoScore;
    private List<AnswerWithDetailsResponse> answers;
}
