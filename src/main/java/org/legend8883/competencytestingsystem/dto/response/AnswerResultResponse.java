package org.legend8883.competencytestingsystem.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class AnswerResultResponse {

    // Нужно для сохранения/отображения оценки HR
    private Long answerId;

    private Long questionId;
    private String questionText;
    private String questionType;
    private Integer questionMaxScore;

    // Для OPEN_ANSWER
    private String userAnswer;

    /**
     * Для вопросов с выбором возвращаем ВСЕ варианты с флагом selected,
     * чтобы фронт мог корректно подсвечивать выбор сотрудника.
     */
    private List<AnswerOptionWithSelectionResponse> selectedOptions;

    private Integer assignedScore;
    private Integer autoScore;
    private Integer finalScore;
    private Boolean isCorrect;
}
