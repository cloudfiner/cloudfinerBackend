package com.aws.exception;

public class AwsConnectionException extends RuntimeException {
    public AwsConnectionException(String msg) {
        super(msg);
    }
}