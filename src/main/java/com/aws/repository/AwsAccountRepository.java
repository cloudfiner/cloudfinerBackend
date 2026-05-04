package com.aws.repository;


import com.aws.entity.AwsAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AwsAccountRepository extends JpaRepository<AwsAccount, UUID> {

    Optional<AwsAccount> findByUserId(UUID userId);

    boolean existsByRoleArn(String roleArn);
}