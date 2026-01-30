package org.legend8883.competencytestingsystem.config;

import org.legend8883.competencytestingsystem.dto.request.CreateTestRequest;
import org.legend8883.competencytestingsystem.entity.User;
import org.legend8883.competencytestingsystem.service.TestService;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;

public class Gok {
    TestService testService;
    ObjectMapper objectMapper = new ObjectMapper();

    private void createDefaultTestForHr(User hr) {
        try {
            InputStream is = new ClassPathResource(
                    "tests/java_test_50_questions.json"
            ).getInputStream();

            CreateTestRequest testRequest =
                    objectMapper.readValue(is, CreateTestRequest.class);

            testService.createTest(testRequest, hr.getId());

        } catch (Exception e) {
            throw new RuntimeException(
                    "Не удалось создать тест для HR", e
            );
        }
    }
}
