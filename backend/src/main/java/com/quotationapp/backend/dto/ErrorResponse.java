package com.quotationapp.backend.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * エラーレスポンスDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private List<FieldErrorDto> fieldErrors;

    public ErrorResponse(int status, String error, String message, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.fieldErrors = null;
    }

    public ErrorResponse(int status, String error, String message, String path, List<FieldErrorDto> fieldErrors) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.fieldErrors = fieldErrors;
    }

    /**
     * エラーレスポンスを作成（メッセージのみ）
     */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(status, error, message, path);
    }

    /**
     * エラーレスポンスを作成（フィールドエラー含む）
     */
    public static ErrorResponse of(int status, String error, String message, String path, List<FieldErrorDto> fieldErrors) {
        return new ErrorResponse(status, error, message, path, fieldErrors);
    }
}

