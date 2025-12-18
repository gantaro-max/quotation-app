package com.quotationapp.backend.converter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import com.quotationapp.backend.dto.QuotationDto;
import com.quotationapp.backend.entity.Quotation;
import com.quotationapp.backend.entity.QuotationItem;

/**
 * 見積EntityとDTOの変換を行うConverterクラス
 */
@Component
public class QuotationConverter {

    /**
     * 見積Entityと明細リストからDTOに変換
     * 
     * @param quotation 見積エンティティ
     * @param items 明細リスト
     * @return 見積DTO
     */
    public QuotationDto toDto(Quotation quotation, List<QuotationItem> items) {
        if (quotation == null) {
            return null;
        }

        QuotationDto dto = new QuotationDto();
        dto.setQuotation(quotation);
        dto.setQuotationItemList(items != null ? items : new ArrayList<>());
        // TODO: 作成者名を取得する場合はUserRepositoryから取得
        // dto.setCreatedByUserName(userRepository.findById(quotation.getCreatedByUserId()).getName());

        return dto;
    }

    /**
     * 見積の新規作成時の初期値を設定
     * 
     * @param quotation 見積エンティティ
     */
    public void setDefaultValues(Quotation quotation) {
        if (quotation == null) {
            return;
        }

        if (quotation.getVersion() == null) {
            quotation.setVersion(1);
        }
        if (quotation.getIsSubmitted() == null) {
            quotation.setIsSubmitted(false);
        }
        if (quotation.getIssueDate() == null) {
            quotation.setIssueDate(LocalDate.now());
        }
        quotation.setCreateAt(LocalDateTime.now());
        quotation.setUpdateAt(LocalDateTime.now());
    }

    /**
     * 見積をコピーして新しい見積エンティティを作成
     * 
     * @param source コピー元の見積エンティティ
     * @param newCreatedByUserId 新規作成者のユーザーID
     * @param newUserDepartmentName 新規作成者の部署名
     * @return 新しい見積エンティティ
     */
    public Quotation copyQuotation(Quotation source, Integer newCreatedByUserId,
            String newUserDepartmentName) {
        if (source == null) {
            return null;
        }

        Quotation newQuotation = new Quotation();
        // 基本情報をコピー
        newQuotation.setEstimateNo(null); // 見積番号は新規生成（後で設定）
        newQuotation.setVersion(1); // 版数は1から
        newQuotation.setIsSubmitted(false); // 提出フラグは未提出
        newQuotation.setCreatedByUserId(newCreatedByUserId);
        newQuotation.setUserDepartmentName(newUserDepartmentName);
        newQuotation.setSalesBranchId(source.getSalesBranchId());
        newQuotation.setSalesStaffId(source.getSalesStaffId());
        newQuotation.setCustomerId(source.getCustomerId());
        newQuotation.setCustomerName(source.getCustomerName());
        newQuotation.setProjectName(source.getProjectName());
        newQuotation.setTotalAmount(source.getTotalAmount());
        newQuotation.setTotalCost(source.getTotalCost());
        newQuotation.setTotalProfit(source.getTotalProfit());
        newQuotation.setProfitRate(source.getProfitRate());
        newQuotation.setGrandTotal(source.getGrandTotal());
        newQuotation.setIssueDate(LocalDate.now()); // 発行日は本日
        newQuotation.setRemarks(source.getRemarks());
        newQuotation.setAttachedFilePath(null); // 添付ファイルはコピーしない

        return newQuotation;
    }

    /**
     * 明細をコピーして新しい明細エンティティを作成
     * 
     * @param source コピー元の明細エンティティ
     * @return 新しい明細エンティティ
     */
    public QuotationItem copyQuotationItem(QuotationItem source) {
        if (source == null) {
            return null;
        }

        QuotationItem newItem = new QuotationItem();
        newItem.setRowOrder(source.getRowOrder());
        newItem.setRowType(source.getRowType());
        newItem.setItemCode(source.getItemCode());
        newItem.setItemName(source.getItemName());
        newItem.setManufacturer(source.getManufacturer());
        newItem.setQuantity(source.getQuantity());
        newItem.setCostPrice(source.getCostPrice());
        newItem.setUnitPrice(source.getUnitPrice());
        // quotationIdは後で設定されるため、ここでは設定しない

        return newItem;
    }

    /**
     * 明細リストをコピーして新しい明細リストを作成
     * 
     * @param sourceItems コピー元の明細リスト
     * @return 新しい明細リスト
     */
    public List<QuotationItem> copyQuotationItems(List<QuotationItem> sourceItems) {
        if (sourceItems == null || sourceItems.isEmpty()) {
            return new ArrayList<>();
        }

        return sourceItems.stream().map(this::copyQuotationItem).collect(Collectors.toList());
    }
}


