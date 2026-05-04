package com.aws.exception;


public class BudgetNotFoundException extends RuntimeException {
    public BudgetNotFoundException(String msg) {
        super(msg);
    }
}