package com.quotationapp.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 見積コピーリクエストDTO
 */
@Data
public class QuotationCopyRequest {
    @NotNull(message = "新規作成者IDは必須です")
    private Integer newCreatedByUserId;
    private String newUserDepartmentName;
}

