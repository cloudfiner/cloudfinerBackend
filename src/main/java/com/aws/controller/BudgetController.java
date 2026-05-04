package com.aws.controller;

import com.aws.dto.BudgetRequest;
import com.aws.dto.BudgetResponse;
import com.aws.service.BudgetService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService service;

    // CREATE / UPDATE
    @PostMapping("/save")
    public ResponseEntity<BudgetResponse> saveOrUpdate(
            @Valid @RequestBody BudgetRequest request) {
               System.err.println(request);
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        BudgetResponse response = service.saveOrUpdateBudget(email, request);

        return ResponseEntity
                .created(URI.create("/api/budgets/" + response.getId()))
                .body(response);
    }

    // GET CURRENT USER BUDGET
    @GetMapping("/getbudget")
    public ResponseEntity<BudgetResponse> getMyBudget() {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        BudgetResponse response = service.getBudgetByEmail(email);

        return ResponseEntity.ok(response);
    }
}