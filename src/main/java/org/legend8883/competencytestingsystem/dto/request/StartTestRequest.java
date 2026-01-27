package org.legend8883.competencytestingsystem.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StartTestRequest {

    @NotNull(message = "ID теста обязательно")
    private Long testId;
}
