package org.legend8883.competencytestingsystem.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TestProgressResponse {
    private Long attemptId;
    private Long testId;
    private String testTitle;
    private LocalDateTime startedAt;
    private LocalDateTime autoSubmitAt;
    private Integer timeLeftMinutes;
    private Integer currentQuestionIndex;
    private Integer totalQuestions;
    private QuestionWithAnswerResponse currentQuestion;
    private List<QuestionProgressResponse> questionProgress;
}