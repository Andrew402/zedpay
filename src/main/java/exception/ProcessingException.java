package com.zedpay.exception;

public class ProcessingException extends ApiException {
    public ProcessingException(String message) {
        super(500, "PROCESSING_ERROR", message);
    }
}