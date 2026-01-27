package org.legend8883.competencytestingsystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "answers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private Attempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    // Для вопросов с выбором: ID выбранных вариантов
    @ElementCollection
    @CollectionTable(name = "selected_options", joinColumns = @JoinColumn(name = "answer_id"))
    @Column(name = "option_id")
    private List<Long> selectedOptionIds = new ArrayList<>();

    // Для открытых вопросов: текстовый ответ
    @Column(name = "open_answer_text", columnDefinition = "TEXT")
    private String openAnswerText;

    // Оценка HR (только для открытых вопросов)
    @Column(name = "assigned_score")
    private Integer assignedScore;

    // Автоматически рассчитанный балл (для вопросов с выбором)
    @Column(name = "auto_score")
    private Integer autoScore;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @PrePersist
    protected void onCreate() {
        answeredAt = LocalDateTime.now();
    }

    // Геттеры для удобства
    public Question getQuestion() {
        return question;
    }

    public Integer getAssignedScore() {
        return assignedScore;
    }

    public Integer getAutoScore() {
        return autoScore;
    }

    @Transient
    public Integer getFinalScore() {
        if (assignedScore != null) {
            return assignedScore;
        }
        return autoScore != null ? autoScore : 0;
    }
}
