package com.aws.entity;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "aws_accounts",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "user_id") // one AWS per user
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AwsAccount {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    private UUID id;

    //  Which user owns this AWS account
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    //  AWS Role ARN
    @Column(name = "role_arn",length = 255)
    private String roleArn;

    //  Security (VERY IMPORTANT)
    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;

    //  AWS Account ID (like 123456789012)
    @Column(name = "aws_account_id", length = 20)
    private String awsAccountId;

    //  Connection status
    @Column(nullable = false)
    private boolean active;

    //  Audit fields (enterprise level)
    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ===========================
    //  AUTO TIMESTAMP HANDLING
    // ===========================
    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.active = false; // default
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}