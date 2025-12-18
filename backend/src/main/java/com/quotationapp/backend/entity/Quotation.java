package com.quotationapp.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 見積ヘッダー Entity Table: quotations
 */
@Data
public class Quotation {
    // システムID
    private Long id;
    // 見積番号
    private String estimateNo;
    // 見積版数
    private Integer version;
    // 提出フラグ(0:未提出,1:提出)
    private Boolean isSubmitted;

    // 作成者情報（メディカル）
    @NotNull(message = "作成者IDは必須です")
    private Integer createdByUserId; // 作成者ID (必須)
    private String userDepartmentName; // 作成時点の部署名（履歴用）
    private Integer salesBranchId;
    private Integer salesStaffId;

    // 顧客情報（NULL許可）
    private Integer customerId;
    @NotBlank(message = "顧客名は必須です")
    private String customerName;

    // 件名
    private String projectName;

    // 金額,利益情報
    private BigDecimal totalAmount; // 税抜き合計
    private BigDecimal totalCost; // 原価合計
    private BigDecimal totalProfit; // 一次利益
    private BigDecimal profitRate; // 利益率
    private BigDecimal grandTotal; // 税込合計金額

    // 発行日
    private LocalDate issueDate;

    // 備考
    private String remarks;

    // 添付ファイルパス
    private String attachedFilePath;

    // システム管理日時
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
