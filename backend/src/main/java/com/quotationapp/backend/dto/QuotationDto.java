package com.quotationapp.backend.dto;

import java.util.List;
import com.quotationapp.backend.entity.Quotation;
import com.quotationapp.backend.entity.QuotationItem;
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
