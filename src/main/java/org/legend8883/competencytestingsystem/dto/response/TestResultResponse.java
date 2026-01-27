package org.legend8883.competencytestingsystem.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TestResultResponse {
    private Long attemptId;
    private Long testId;
    private String testTitle;
    private UserSimpleResponse user;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer score;
    private Integer maxPossibleScore;
    private Integer passingScore;
    private String status;
    private Boolean passed;
    private Integer correctAnswersCount;
    private Integer totalQuestions;
    private List<AnswerResultResponse> answers;
}