package com.aws.controller;

import com.aws.dto.AwsAccountResponseDto;
import com.aws.dto.AwsConnectRequestDto;
import com.aws.dto.AwsConnectResponseDto;
import com.aws.dto.AwsSetupResponseDto;
import com.aws.entity.AwsAccount;
import com.aws.service.AwsAccountService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;





@RestController
@RequestMapping("/api/aws")
@RequiredArgsConstructor
public class AwsAccountController {

    private final AwsAccountService service;
    private static final Logger log = LoggerFactory.getLogger(AwsAccountController.class);

    @GetMapping("/setup")
    public ResponseEntity<AwsSetupResponseDto> setup(
            @AuthenticationPrincipal(expression = "id") UUID userId) {

        validateUser(userId);

        return ResponseEntity.ok(service.generateSetup(userId));
    }

    @PostMapping("/connect")
    public ResponseEntity<AwsConnectResponseDto> connect(
            @AuthenticationPrincipal(expression = "id") UUID userId,
            @Valid @RequestBody AwsConnectRequestDto request) {

        validateUser(userId);

        log.info("Connecting AWS for user: {}", userId);

        String message = service.connectAws(userId, request.getRoleArn());

        return ResponseEntity.ok(new AwsConnectResponseDto(message));
    }

    @GetMapping
    public ResponseEntity<AwsAccountResponseDto> getAccount(
            @AuthenticationPrincipal(expression = "id") UUID userId) {

        validateUser(userId);

        AwsAccount account = service.getByUserId(userId);

        return ResponseEntity.ok(
                new AwsAccountResponseDto(
                        account.getUserId(),
                        account.getRoleArn(),
                        account.isActive()
                )
        );
    }

    private void validateUser(UUID userId) {
        if (userId == null) {
            throw new RuntimeException("Unauthorized");
        }
    }
}