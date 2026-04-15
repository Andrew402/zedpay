package com.zedpay.exception;

import java.time.LocalDateTime;

public class ApiError {
    private final String errorCode;
    private final String message;
    private final String timestamp;

    public ApiError(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
        this.timestamp = LocalDateTime.now().toString();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }
}