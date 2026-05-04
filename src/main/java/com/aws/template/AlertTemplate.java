package com.aws.template;

public enum AlertTemplate {

    COST_ALERT("Your AWS cost exceeded ₹%s"),
    LOGIN_ALERT("New login detected"),
    SERVER_DOWN("Server is down"),
    PAYMENT_FAILED("Payment failed for user"),
    LOW_BALANCE("Your balance is low");

    private final String template;

    AlertTemplate(String template) {
        this.template = template;
    }

    public String format(Object... args) {
        return String.format(template, args);
    }
}