package com.aws.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "telegram_links")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TelegramLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // 🔗 Proper relation with User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    //  Unique token
    @Column(nullable = false, unique = true)
    private String token;

    //  Telegram chatId
    @Column(name = "chat_id")
    private String chatId;

    //  Token used or not
    private boolean used;

    // Expiry time
    @Column(nullable = false)
    private LocalDateTime expiry;

    //  Audit fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    //  Auto set timestamps
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}