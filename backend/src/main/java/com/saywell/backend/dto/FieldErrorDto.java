package com.saywell.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * フィールドエラー情報DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldErrorDto {
    private String field;
    private Object rejectedValue;
    private String message;

    public FieldErrorDto(String field, String message) {
        this.field = field;
        this.message = message;
        this.rejectedValue = null;
    }
}




