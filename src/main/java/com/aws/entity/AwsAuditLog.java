package com.aws.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "aws_audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AwsAuditLog {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID userId;

    private String action; // CONNECT / FAILED

    private String awsAccountId;

    private String message;

    private LocalDateTime createdAt;
}