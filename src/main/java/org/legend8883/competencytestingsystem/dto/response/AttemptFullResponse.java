package org.legend8883.competencytestingsystem.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AttemptFullResponse {
    private Long id;
    private Long testId;
    private String testTitle;
    private UserSimpleResponse user;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer score;
    private String status;
    private Boolean passed;
    private Integer maxPossibleScore;
    private Integer passingScore;
    private List<AnswerFullResponse> answers;
    private List<AnswerFullResponse> openAnswers; // Только для статуса EVALUATING
}
