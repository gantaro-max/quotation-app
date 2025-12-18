package com.saywell.backend.dto;

import java.util.List;
import com.saywell.backend.entity.Quotation;
import com.saywell.backend.entity.QuotationItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 見積新規作成リクエストDTO
 */
@Data
public class QuotationCreateRequest {
    @NotNull(message = "見積情報は必須です")
    @Valid
    private Quotation quotation;
    
    @Valid
    private List<QuotationItem> items;
}

