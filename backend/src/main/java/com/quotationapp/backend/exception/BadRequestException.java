package com.quotationapp.backend.exception;

/**
 * 不正なリクエストの場合の例外
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}




