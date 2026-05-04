package com.aws.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "alert_executions")
public class AlertExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID ruleId;
    private UUID userId;

    private boolean lastState;

    private LocalDateTime lastTriggeredAt;
    private LocalDateTime lastResolvedAt;

    // 🔥 escalation level
    private int triggerCount;

    // getters setters
}