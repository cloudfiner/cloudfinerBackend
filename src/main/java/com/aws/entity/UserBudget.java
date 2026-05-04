package com.aws.entity;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "user_budget",
    indexes = {
        @Index(name = "idx_email", columnList = "email")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBudget {

    @Id
    @GeneratedValue
    private UUID id;

    // 🔥 user identify (from JWT)
    @Column(nullable = false, unique = true)
    private String email;

    // 🔥 budget value
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyBudget;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}