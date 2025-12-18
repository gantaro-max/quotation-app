package com.quotationapp.backend.dto;

import java.util.List;
import com.quotationapp.backend.entity.Quotation;
import com.quotationapp.backend.entity.QuotationItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 見積更新リクエストDTO
 */
@Data
public class QuotationUpdateRequest {
    @NotNull(message = "見積情報は必須です")
    @Valid
    private Quotation quotation;
    
    @Valid
    private List<QuotationItem> items;
}

