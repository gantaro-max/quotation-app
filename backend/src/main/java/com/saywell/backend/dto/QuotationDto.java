package com.saywell.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.saywell.backend.entity.QuotationItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuotationDto {

    // --- ID・基本情報 ---
    private Long id; // 新規時はnull

    private String estimateNo;

    private Integer version;
    private Boolean isSubmitted;

    // --- 関連ID (入力用) ---
    @NotNull(message = "作成者は必須です")
    private Integer createdByUserId;

    // ※以下はNULL許可
    private Integer salesBranchId;
    private Integer salesStaffId;
    private Integer customerId;

    // --- 関連オブジェクト (表示・印刷用 / ネスト) ---
    // 保存リクエスト時には null で送られてきてもOK (無視するだけ)
    private String createdByUserName;
    private UserDto createdByUser;
    private BranchDto salesBranch;
    private SalesStaffDto salesStaff;
    private CustomerDto customer;

    // --- 案件情報 ---
    @NotBlank(message = "顧客名は必須です")
    private String customerName; // スナップショット

    private String projectName;
    private LocalDate issueDate;
    private String remarks;

    // --- 金額系 ---
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalCost;
    private BigDecimal totalProfit;
    private BigDecimal profitRate;
    private BigDecimal grandTotal;

    // --- 明細リスト ---
    @Valid // リストの中身もバリデーション
    private List<QuotationItem> items;

    // --- 管理日時 ---
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
