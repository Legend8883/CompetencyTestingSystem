package org.legend8883.competencytestingsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Test {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Название теста обязательно")
    @Size(min = 3, max = 200, message = "Название должно быть от 3 до 200 символов")
    @Column(nullable = false)
    private String title;

    @Size(max = 1000, message = "Описание не должно превышать 1000 символов")
    private String description;

    @NotNull(message = "Время на прохождение обязательно")
    @Min(value = 5, message = "Минимальное время - 5 минут")
    @Max(value = 180, message = "Максимальное время - 180 минут")
    @Column(name = "time_limit_minutes", nullable = false)
    private Integer timeLimitMinutes;

    @NotNull(message = "Проходной балл обязателен")
    @Min(value = 0, message = "Проходной балл не может быть отрицательным")
    @Column(name = "passing_score", nullable = false)
    private Integer passingScore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Связь с вопросами
    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<Question> questions = new ArrayList<>();
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Transient
    public Integer getMaxPossibleScore() {
        return questions.stream()
                .mapToInt(Question::getMaxScore)
                .sum();
    }
}
