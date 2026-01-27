package org.legend8883.competencytestingsystem.dto.response;

import lombok.Data;

@Data
public class QuestionProgressResponse {
    private Long questionId;
    private Integer orderIndex;
    private Boolean answered;
    private Boolean visited;
}
