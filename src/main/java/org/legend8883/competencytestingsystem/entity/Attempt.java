package org.legend8883.competencytestingsystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "attempts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Attempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "score")
    private Integer score = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @Column(name = "auto_submit_at")
    private LocalDateTime autoSubmitAt;

    // Добавляем связь с ответами
    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Answer> answers = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        startedAt = LocalDateTime.now();
        if (test != null && test.getTimeLimitMinutes() != null) {
            autoSubmitAt = startedAt.plusMinutes(test.getTimeLimitMinutes());
            System.out.println("DEBUG: Setting autoSubmitAt = " + autoSubmitAt +
                    " (startedAt = " + startedAt +
                    ", timeLimit = " + test.getTimeLimitMinutes() + " minutes)");
        } else {
            System.out.println("WARNING: Test or timeLimitMinutes is null!");
        }
    }

    // Геттер для удобства
    public List<Answer> getAnswers() {
        return answers != null ? answers : new ArrayList<>();
    }
}
