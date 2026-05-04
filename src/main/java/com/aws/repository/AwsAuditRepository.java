package com.aws.repository;



import com.aws.entity.AwsAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AwsAuditRepository extends JpaRepository<AwsAuditLog, UUID> {

    // 🔥 Get all logs for a user
    List<AwsAuditLog> findByUserId(UUID userId);

    // 🔥 Get latest logs (optional)
    List<AwsAuditLog> findTop10ByUserIdOrderByCreatedAtDesc(UUID userId);
}