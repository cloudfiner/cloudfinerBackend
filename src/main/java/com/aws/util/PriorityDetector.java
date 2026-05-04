package com.aws.util;

public class PriorityDetector {

    public static String detect(String message) {

        if (message == null) return "INFO";

        String msg = message.toLowerCase();

        if (msg.contains("error") || msg.contains("failed") || msg.contains("down")) {
            return "ERROR";
        }

        if (msg.contains("warning") || msg.contains("low") || msg.contains("limit")) {
            return "WARNING";
        }

        return "INFO";
    }
}