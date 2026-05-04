package com.aws.serviceImpl;

import com.aws.dto.AwsSetupResponseDto;
import com.aws.entity.AwsAccount;
import com.aws.entity.AwsAuditLog;
import com.aws.exception.AwsConnectionException;
import com.aws.repository.AwsAccountRepository;
import com.aws.repository.AwsAuditRepository;
import com.aws.service.AwsAccountService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwsAccountServiceImpl implements AwsAccountService {

    private final AwsAccountRepository repository;
    private final AwsAuditRepository auditRepository;
    private final StsClient stsClient;
    private final RateLimitService rateLimitService;

    private static final Pattern ARN_PATTERN =
            Pattern.compile("^arn:aws:iam::\\d{12}:role\\/[a-zA-Z0-9+=,.@_-]+$");

    @Value("${aws.account.id}")
    private String accountId;

    @Value("${aws.session.name:cloud-cost-session}")
    private String sessionName;

    // ===========================
    // 1. SETUP
    // ===========================
    @Override
    @Transactional
    public AwsSetupResponseDto generateSetup(UUID userId) {

        log.info("Generating AWS setup for user {}", userId);

        AwsAccount account = repository.findByUserId(userId)
                .orElseGet(() -> {
                    AwsAccount newAccount = new AwsAccount();
                    newAccount.setUserId(userId);
                    newAccount.setExternalId(UUID.randomUUID().toString());
                    newAccount.setActive(false);
                    return repository.save(newAccount);
                });

        if (account.getExternalId() == null || account.getExternalId().isBlank()) {
            account.setExternalId(UUID.randomUUID().toString());
            repository.save(account);
        }

        if (accountId == null || accountId.isBlank()) {
            throw new IllegalStateException("AWS Account ID not configured");
        }

        String policy = String.format("""
            {
              "Version": "2012-10-17",
              "Statement": [
                {
                  "Effect": "Allow",
                  "Principal": {
                    "AWS": "arn:aws:iam::%s:root"
                  },
                  "Action": "sts:AssumeRole",
                  "Condition": {
                    "StringEquals": {
                      "sts:ExternalId": "%s"
                    }
                  }
                }
              ]
            }
            """, accountId, account.getExternalId());

        log.debug("Generated externalId for user {}: {}", userId, account.getExternalId());

        return AwsSetupResponseDto.builder()
                .accountId(accountId)
                .externalId(account.getExternalId())
                .policy(policy)
                .success(true)
                .instructions("Create IAM Role → Attach policy → Paste Role ARN")
                .build();
    }

    // ===========================
    // 2. CONNECT
    // ===========================
    @Override
    @CircuitBreaker(name = "awsService", fallbackMethod = "fallback")
    public String connectAws(UUID userId, String roleArn) {

        rateLimitService.checkRateLimit(userId);

        if (roleArn == null || !ARN_PATTERN.matcher(roleArn).matches()) {
            throw new AwsConnectionException("Invalid AWS Role ARN");
        }

        AwsAccount account = repository.findByUserId(userId)
                .orElseThrow(() -> new AwsConnectionException("Run setup first"));

        if (account.getExternalId() == null || account.getExternalId().isBlank()) {
            throw new AwsConnectionException("ExternalId missing");
        }

        if (account.isActive() && roleArn.equals(account.getRoleArn())) {
            log.info("AWS already connected for user {}", userId);
            return "AWS already connected";
        }

        try {

            AssumeRoleResponse response =
                    assumeRole(roleArn, account.getExternalId());

            Credentials c = response.credentials();

            AwsSessionCredentials sessionCreds = AwsSessionCredentials.create(
                    c.accessKeyId(),
                    c.secretAccessKey(),
                    c.sessionToken()
            );

            String awsAccountId;

            try (StsClient tempClient = StsClient.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(sessionCreds))
                    .overrideConfiguration(
                            ClientOverrideConfiguration.builder()
                                    .apiCallTimeout(Duration.ofSeconds(5))
                                    .build()
                    )
                    .build()) {

                awsAccountId = tempClient.getCallerIdentity().account();
            }

            if (awsAccountId == null || awsAccountId.isBlank()) {
                throw new AwsConnectionException("Unable to verify AWS account");
            }

            updateAccount(account, roleArn, awsAccountId);

            saveAudit(userId, "SUCCESS", awsAccountId, "AWS connected successfully");

            log.info("AWS connected for user {} account {}", userId, awsAccountId);

            return "AWS Connected Successfully";

        } catch (Exception e) {

            log.error("AWS connection failed for user {}", userId, e);

            saveAudit(userId, "FAILED", null, "AWS connection failed");

            throw new AwsConnectionException("AWS connection failed");
        }
    }

    // ===========================
    // 3. ASSUME ROLE (RETRY)
    // ===========================
    @Retry(name = "awsRetry")
    public AssumeRoleResponse assumeRole(String roleArn, String externalId) {

        AssumeRoleResponse response = stsClient.assumeRole(
                AssumeRoleRequest.builder()
                        .roleArn(roleArn)
                        .externalId(externalId)
                        .roleSessionName(sessionName)
                        .build()
        );

        if (response == null || response.credentials() == null) {
            throw new AwsConnectionException("Permission denied");
        }

        return response;
    }

    // ===========================
    // 4. UPDATE ACCOUNT (Transactional)
    // ===========================
    @Transactional
    public void updateAccount(AwsAccount account, String roleArn, String awsAccountId) {
        account.setRoleArn(roleArn);
        account.setAwsAccountId(awsAccountId);
        account.setActive(true);
        repository.save(account);
    }

    // ===========================
    // 5. AUDIT
    // ===========================
    private void saveAudit(UUID userId, String action, String awsAccountId, String message) {
        auditRepository.save(
                AwsAuditLog.builder()
                        .userId(userId)
                        .action(action)
                        .awsAccountId(awsAccountId)
                        .message(message)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }

    // ===========================
    // 6. FALLBACK
    // ===========================
    public String fallback(UUID userId, String roleArn, Throwable t) {
        log.error("Circuit breaker triggered for user {}", userId, t);
        throw new AwsConnectionException("AWS service temporarily unavailable");
    }

    // ===========================
    // 7. GET ACCOUNT
    // ===========================
    @Override
    public AwsAccount getByUserId(UUID userId) {

        Optional<AwsAccount> opt = repository.findByUserId(userId);

        if (opt.isEmpty()) {
            throw new AwsConnectionException("AWS not connected");
        }

        return opt.get();
    }
}