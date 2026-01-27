package org.legend8883.competencytestingsystem.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TestResponse {
    private Long id;
    private String title;
    private String description;
    private Integer timeLimitMinutes;
    private Integer passingScore;
    private Integer maxPossibleScore;
    private Integer questionCount;
    private UserSimpleResponse createdBy;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private List<QuestionResponse> questions;
}