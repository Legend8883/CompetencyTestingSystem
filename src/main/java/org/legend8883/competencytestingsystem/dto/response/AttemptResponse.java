package org.legend8883.competencytestingsystem.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttemptResponse {
    private Long id;
    private Long testId;
    private String testTitle;
    private UserSimpleResponse user;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer score;
    private String status;
    private Boolean passed;
}
