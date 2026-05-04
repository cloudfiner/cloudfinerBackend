package com.aws.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

import com.aws.template.AlertType;
import com.aws.template.ConditionType;


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "alert_rules")
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type")   // ✅ safe naming
    private AlertType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type") 
    private ConditionType condition;

    private BigDecimal threshold;

    private String templateKey;

    private boolean active = true;

    private String priority;

    private int cooldownMinutes = 10;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}