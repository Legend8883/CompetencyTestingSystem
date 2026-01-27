package org.legend8883.competencytestingsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Текст вопроса обязателен")
    @Size(min = 5, max = 1000, message = "Текст вопроса должен быть от 5 до 1000 символов")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType type;

    // Для открытых вопросов
    @Size(max = 5000, message = "Ответ не должен превышать 5000 символов")
    @Column(name = "correct_open_answer", columnDefinition = "TEXT")
    private String correctOpenAnswer;

    // Варианты ответов (только для типов SINGLE_CHOICE и MULTIPLE_CHOICE)
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<AnswerOption> options = new ArrayList<>();

    @NotNull(message = "Максимальный балл обязателен")
    @Min(value = 1, message = "Минимальный балл - 1")
    @Max(value = 100, message = "Максимальный балл - 100")
    @Column(name = "max_score", nullable = false)
    private Integer maxScore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @Column(name = "order_index")
    private Integer orderIndex = 0;

    // Метод для получения правильных вариантов
    @Transient
    public List<AnswerOption> getCorrectOptions() {
        return options.stream()
                .filter(AnswerOption::getIsCorrect)
                .collect(Collectors.toList());
    }

    // Метод для проверки типа
    @Transient
    public boolean isChoiceQuestion() {
        return type == QuestionType.SINGLE_CHOICE || type == QuestionType.MULTIPLE_CHOICE;
    }
}