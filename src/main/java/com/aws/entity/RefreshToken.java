package com.aws.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(
    name = "refresh_tokens",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_username", columnNames = "username") // 🔥 1 user = 1 token
    },
    indexes = {
        @Index(name = "idx_token", columnList = "token"),
        @Index(name = "idx_username", columnList = "username")
    }
)
public class RefreshToken {

    @Id
    @GeneratedValue
    private UUID id;

    // 🔥 Token must be unique
    @Column(nullable = false, unique = true, length = 500)
    private String token;

    // 🔥 IMPORTANT: now unique
    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private Instant expiryDate;

    @Column(nullable = false)
    private boolean revoked = false;

    // 🔥 Audit fields
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant updatedAt;

    // Optional (keep for future)
    private String device;
    private String ipAddress;

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}