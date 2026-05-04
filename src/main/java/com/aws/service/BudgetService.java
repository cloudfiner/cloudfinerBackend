package com.aws.service;


import com.aws.dto.BudgetRequest;
import com.aws.dto.BudgetResponse;


public interface BudgetService {

    // CREATE / UPDATE (email comes from JWT)
    BudgetResponse saveOrUpdateBudget(String email, BudgetRequest request);

    //  GET CURRENT USER BUDGET
    BudgetResponse getBudgetByEmail(String email);
}