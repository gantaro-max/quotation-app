package com.saywell.backend.dto;

import java.util.List;
import com.saywell.backend.entity.Quotation;
import com.saywell.backend.entity.QuotationItem;
import lombok.Data;

/**
 * 見積DTO APIレスポンス用
 */
@Data
public class QuotationDto {

    private Quotation quotation;
    private List<QuotationItem> quotationItemList;

    private String createdByUserName; // 作成者名



}
