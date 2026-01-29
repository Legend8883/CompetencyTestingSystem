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

    // Для вопросов с выбором: выбранные варианты
    @OneToMany(mappedBy = "answer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<UserSelectedOption> selectedOptions = new ArrayList<>();

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

    // Геттер для удобства - преобразует selectedOptions в List<Long>
    @Transient
    public List<Long> getSelectedOptionIds() {
        return selectedOptions.stream()
                .map(UserSelectedOption::getOptionId)
                .toList();
    }

    // Сеттер для удобства - преобразует List<Long> в selectedOptions
    @Transient
    public void setSelectedOptionIds(List<Long> optionIds) {
        this.selectedOptions.clear();
        if (optionIds != null) {
            for (Long optionId : optionIds) {
                UserSelectedOption userOption = new UserSelectedOption();
                userOption.setAnswer(this);
                userOption.setOptionId(optionId);
                this.selectedOptions.add(userOption);
            }
        }
    }
}
