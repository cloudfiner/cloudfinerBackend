package com.aws.serviceImpl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aws.dto.BudgetRequest;
import com.aws.dto.BudgetResponse;
import com.aws.entity.UserBudget;
import com.aws.exception.BudgetNotFoundException;
import com.aws.repository.UserBudgetRepository;
import com.aws.service.BudgetService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private final UserBudgetRepository repo;

    @Override
    @Transactional
    public BudgetResponse saveOrUpdateBudget(String email, BudgetRequest request) {

        validateRequest(request);

        Optional<UserBudget> optionalBudget = repo.findByEmail(email);

        UserBudget budget;
        String message;

        if (optionalBudget.isPresent()) {
            // 🔄 UPDATE CASE
            budget = optionalBudget.get();
            budget.setMonthlyBudget(request.getMonthlyBudget());
            budget.setUpdatedAt(LocalDateTime.now());

            message = "Budget updated successfully"; // ✅ update msg

        } else {
            // 🆕 CREATE CASE
            budget = UserBudget.builder()
                    .email(email)
                    .monthlyBudget(request.getMonthlyBudget())
                    .createdAt(LocalDateTime.now())
                    .build();

            message = "Budget created successfully"; // ✅ create msg
        }

        UserBudget saved = repo.save(budget);

        return mapToResponse(saved, message);
    }
    @Override
    @Transactional(readOnly = true)
    public BudgetResponse getBudgetByEmail(String email) {

        return repo.findByEmail(email)
                .map(budget -> mapToResponse(budget, "Budget fetched successfully"))
                .orElseGet(() -> BudgetResponse.builder()
                        .email(email)
                        .monthlyBudget(BigDecimal.ZERO)
                        .createdAt(null)
                        .updatedAt(null)
                        .status("SUCCESS")
                        .message("No budget set")
                        .build());
    }

    // ------------------ PRIVATE METHODS ------------------

    private void validateRequest(BudgetRequest request) {

        if (request.getMonthlyBudget() == null ||
                request.getMonthlyBudget().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Budget must be greater than 0");
        }
    }

    private BudgetResponse mapToResponse(UserBudget budget, String message) {
        return BudgetResponse.builder()
                .id(budget.getId())
                .email(budget.getEmail())
                .monthlyBudget(budget.getMonthlyBudget())
                .createdAt(budget.getCreatedAt())
                .updatedAt(budget.getUpdatedAt())
                .status("SUCCESS")
                .message(message) //  dynamic message
                .build();
    }
}