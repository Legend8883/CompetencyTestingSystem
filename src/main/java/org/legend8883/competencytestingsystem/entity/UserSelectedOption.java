package org.legend8883.competencytestingsystem.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "user_selected_options")
@Data
public class UserSelectedOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id", nullable = false)
    private Answer answer;

    @Column(name = "option_id", nullable = false)
    private Long optionId;
}