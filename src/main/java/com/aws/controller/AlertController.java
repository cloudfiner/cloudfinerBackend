package com.aws.controller;

import com.aws.dto.AlertResponseDto;
import com.aws.dto.ApiResponse;
import com.aws.service.AlertConfigService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertConfigService alertConfigService;

    // ================= CREATE =================
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<AlertResponseDto>> createAlert(
            @RequestParam double threshold
    ) {

        AlertResponseDto alert = alertConfigService.createAlert(threshold);

        ApiResponse<AlertResponseDto> response =
                new ApiResponse<>(true, "Alert created successfully", alert);

        return ResponseEntity.status(201).body(response);
    }

    // ================= GET ALL =================
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<AlertResponseDto>>> getUserAlerts(
            @RequestParam(defaultValue = "real") String type
    ) {

        List<AlertResponseDto> alerts = alertConfigService.getUserAlerts(type);

        ApiResponse<List<AlertResponseDto>> response =
                new ApiResponse<>(true, "Alerts fetched successfully", alerts);

        return ResponseEntity.ok(response);
    }
    // ================= DELETE =================
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAlert(@PathVariable UUID id) {

        alertConfigService.deleteAlert(id);

        ApiResponse<Void> response =
                new ApiResponse<>(true, "Alert deleted successfully", null);

        return ResponseEntity.ok(response);
    }

    // ================= TOGGLE =================
    @PutMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<AlertResponseDto>> toggleAlert(@PathVariable UUID id) {

        AlertResponseDto alert = alertConfigService.toggleAlert(id);

        ApiResponse<AlertResponseDto> response =
                new ApiResponse<>(true, "Alert toggled successfully", alert);

        return ResponseEntity.ok(response);
    }
}