package com.saywell.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.saywell.backend.dto.QuotationDto;
import com.saywell.backend.entity.Quotation;
import com.saywell.backend.entity.QuotationItem;
import com.saywell.backend.exception.ResourceNotFoundException;
import com.saywell.backend.exception.UnauthorizedException;
import com.saywell.backend.repository.QuotationRepository;
import lombok.RequiredArgsConstructor;

/**
 * 見積管理サービス ビジネスロジックをここに集約します。
 */
@Service
@RequiredArgsConstructor
public class QuotationService {

    private final QuotationRepository quotationRepository;

    // =========================================================================
    // 参照系 (Read Only)
    // =========================================================================

    /**
     * IDで見積DTOを取得 (画面表示・プレビュー用)
     */
    @Transactional(readOnly = true)
    public QuotationDto findDtoById(Long id) {
        return quotationRepository.findDtoById(id)
                .orElseThrow(() -> new ResourceNotFoundException("見積が見つかりません。ID: " + id));
    }

    /**
     * 見積検索 (一覧画面用)
     */
    @Transactional(readOnly = true)
    public List<QuotationDto> search(String customerName, String projectName, String estimateNo) {
        return quotationRepository.search(customerName, projectName, estimateNo);
    }

    /**
     * 作成者IDで見積一覧を取得
     */
    @Transactional(readOnly = true)
    public List<QuotationDto> findByCreatedByUserId(Integer createdByUserId) {
        return quotationRepository.findByCreatedByUserId(createdByUserId);
    }

    // ★削除: findLoginDtoByEmail は UserService へ移動するため削除しました

    // =========================================================================
    // 更新系 (Transactional)
    // =========================================================================

    /**
     * 見積を新規保存
     */
    @Transactional
    public QuotationDto create(QuotationDto dto) {
        Quotation quotation = toEntity(dto);

        quotation.setId(null);
        quotation.setCreatedAt(LocalDateTime.now());
        quotation.setUpdatedAt(LocalDateTime.now());

        quotationRepository.insert(quotation);
        Long newId = quotation.getId();

        saveItems(newId, dto.getItems());

        return findDtoById(newId);
    }

    /**
     * 見積を更新保存
     */
    @Transactional
    public QuotationDto update(Long id, QuotationDto dto, Integer currentUserId) {
        Quotation existing = quotationRepository.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("見積が見つかりません。ID: " + id);
        }
        if (!existing.getCreatedByUserId().equals(currentUserId)) {
            throw new UnauthorizedException("権限エラー: 作成者本人以外の見積は編集できません。");
        }

        Quotation updateEntity = toEntity(dto);
        updateEntity.setId(id);

        updateEntity.setCreatedByUserId(existing.getCreatedByUserId());
        updateEntity.setUserDepartmentName(existing.getUserDepartmentName());
        updateEntity.setCreatedAt(existing.getCreatedAt());
        updateEntity.setUpdatedAt(LocalDateTime.now());

        quotationRepository.update(updateEntity);

        quotationRepository.deleteItemsByQuotationId(id);
        saveItems(id, dto.getItems());

        return findDtoById(id);
    }

    /**
     * 見積を削除
     */
    @Transactional
    public void delete(Long id, Integer currentUserId) {
        Quotation existing = quotationRepository.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("見積が見つかりません。ID: " + id);
        }
        if (!existing.getCreatedByUserId().equals(currentUserId)) {
            throw new UnauthorizedException("権限エラー: 作成者本人以外の見積は削除できません。");
        }

        quotationRepository.deleteItemsByQuotationId(id);
        quotationRepository.delete(id, currentUserId);
    }

    /**
     * 見積コピー
     */
    @Transactional
    public QuotationDto copy(Long sourceId, Integer newCreatedByUserId,
            String newUserDepartmentName) {
        QuotationDto source = findDtoById(sourceId);

        QuotationDto newDto = new QuotationDto();
        newDto.setEstimateNo(generateNewEstimateNo());
        newDto.setVersion(1);
        newDto.setIsSubmitted(false);
        newDto.setCreatedByUserId(newCreatedByUserId);

        newDto.setSalesBranchId(source.getSalesBranchId());
        newDto.setSalesStaffId(source.getSalesStaffId());
        newDto.setCustomerId(source.getCustomerId());
        newDto.setCustomerName(source.getCustomerName());
        newDto.setProjectName("【コピー】" + source.getProjectName());

        newDto.setIssueDate(null);
        newDto.setRemarks(source.getRemarks());

        newDto.setTotalAmount(source.getTotalAmount());
        newDto.setTotalCost(source.getTotalCost());
        newDto.setTotalProfit(source.getTotalProfit());
        newDto.setProfitRate(source.getProfitRate());
        newDto.setGrandTotal(source.getGrandTotal());

        if (source.getItems() != null) {
            List<QuotationItem> newItems = source.getItems().stream().map(item -> {
                QuotationItem newItem = new QuotationItem();
                newItem.setRowOrder(item.getRowOrder());
                newItem.setRowType(item.getRowType());
                newItem.setItemCode(item.getItemCode());
                newItem.setItemName(item.getItemName());
                newItem.setManufacturer(item.getManufacturer());
                newItem.setQuantity(item.getQuantity());
                newItem.setCostPrice(item.getCostPrice());
                newItem.setUnitPrice(item.getUnitPrice());
                return newItem;
            }).collect(Collectors.toList());
            newDto.setItems(newItems);
        }

        return create(newDto);
    }

    // =========================================================================
    // Private Helper Methods
    // =========================================================================

    private void saveItems(Long quotationId, List<QuotationItem> items) {
        if (items != null && !items.isEmpty()) {
            for (QuotationItem item : items) {
                item.setId(null);
                item.setQuotationId(quotationId);
            }
            quotationRepository.insertItems(items);
        }
    }

    private Quotation toEntity(QuotationDto dto) {
        Quotation q = new Quotation();
        q.setEstimateNo(dto.getEstimateNo());
        q.setVersion(dto.getVersion());
        q.setIsSubmitted(dto.getIsSubmitted());
        q.setCreatedByUserId(dto.getCreatedByUserId());
        q.setSalesBranchId(dto.getSalesBranchId());
        q.setSalesStaffId(dto.getSalesStaffId());
        q.setCustomerId(dto.getCustomerId());
        q.setCustomerName(dto.getCustomerName());
        q.setProjectName(dto.getProjectName());
        q.setTotalAmount(dto.getTotalAmount());
        q.setTotalCost(dto.getTotalCost());
        q.setTotalProfit(dto.getTotalProfit());
        q.setProfitRate(dto.getProfitRate());
        q.setGrandTotal(dto.getGrandTotal());
        q.setIssueDate(dto.getIssueDate());
        q.setRemarks(dto.getRemarks());
        return q;
    }

    private String generateNewEstimateNo() {
        return "Q" + System.currentTimeMillis();
    }
}
