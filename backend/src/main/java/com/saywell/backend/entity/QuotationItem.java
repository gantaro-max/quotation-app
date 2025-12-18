package com.saywell.backend.entity;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 見積明細 Entity Table: quotation_item
 */
@Data
public class QuotationItem {

    // 明細ID
    private Long id;

    // 見積ヘッダID(FK)
    private Long quotationId;

    // 表示順
    @NotNull(message = "表示順は必須です")
    private Integer rowOrder;

    // 行タイプ
    @NotBlank(message = "行タイプは必須です")
    @Pattern(regexp = "normal|manufacturer|note|detail", message = "行タイプは normal, manufacturer, note, detail のいずれかである必要があります")
    private String rowType;

    // 商品コード(9桁文字列)
    private String itemCode;

    // 商品名・概要
    private String itemName;

    // メーカー名
    private String manufacturer;

    // 数量
    private BigDecimal quantity;

    // 仕切単価
    private BigDecimal costPrice;

    // 提出単価
    private BigDecimal unitPrice;

}
