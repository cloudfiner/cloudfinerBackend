package com.aws.service;

public interface MessageService {

    void sendAlert(String message, String chatId);
}