package com.hrs.admin.exception;

public class BusinessException extends RuntimeException {
    private final int httpStatus;

    public BusinessException(String message) {
        this(message, 422);
    }

    public BusinessException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
