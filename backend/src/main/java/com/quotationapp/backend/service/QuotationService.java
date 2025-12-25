package com.quotationapp.backend.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.quotationapp.backend.dto.QuotationDto;
import com.quotationapp.backend.entity.Quotation;
import com.quotationapp.backend.entity.QuotationItem;
import com.quotationapp.backend.exception.ResourceNotFoundException;
import com.quotationapp.backend.exception.UnauthorizedException;
import com.quotationapp.backend.repository.QuotationRepository;
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

    // =========================================================================
    // 更新系 (Transactional)
    // =========================================================================

    /**
     * 見積を新規保存
     */
    @Transactional
    public QuotationDto create(QuotationDto dto) {
        // ▼▼▼ 追加: フロントから estimateNo が null または空で来たら自動採番する ▼▼▼
        if (dto.getEstimateNo() == null || dto.getEstimateNo().isEmpty()) {
            dto.setEstimateNo(generateNewEstimateNo());
        }
        // ▲▲▲ 追加終わり ▲▲▲

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
        // 作成者本人チェック (必要に応じてコメントアウト可)
        if (!existing.getCreatedByUserId().equals(currentUserId)) {
            throw new UnauthorizedException("権限エラー: 作成者本人以外の見積は編集できません。");
        }

        Quotation updateEntity = toEntity(dto);
        updateEntity.setId(id);

        // 変更してはいけないフィールドを既存データから維持
        updateEntity.setCreatedByUserId(existing.getCreatedByUserId());
        updateEntity.setUserDepartmentName(existing.getUserDepartmentName());
        updateEntity.setCreatedAt(existing.getCreatedAt());
        updateEntity.setUpdatedAt(LocalDateTime.now());

        quotationRepository.update(updateEntity);

        // 明細はいったん全削除して再登録 (シンプルな実装)
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
     * 見積コピー (別案件としてコピーする場合)
     */
    @Transactional
    public QuotationDto copy(Long sourceId, Integer newCreatedByUserId,
            String newUserDepartmentName) {
        QuotationDto source = findDtoById(sourceId);

        QuotationDto newDto = new QuotationDto();
        // コピー時は常に新しい番号を発行
        newDto.setEstimateNo(generateNewEstimateNo());
        newDto.setVersion(1);
        newDto.setIsSubmitted(false);
        newDto.setCreatedByUserId(newCreatedByUserId);

        newDto.setSalesBranchId(source.getSalesBranchId());
        newDto.setSalesStaffId(source.getSalesStaffId());
        newDto.setCustomerId(source.getCustomerId());
        newDto.setCustomerName(source.getCustomerName());

        // 案件名をそのまま引き継ぐ
        newDto.setProjectName(source.getProjectName());

        newDto.setIssueDate(null);
        newDto.setRemarks(source.getRemarks());

        newDto.setTotalAmount(source.getTotalAmount());
        newDto.setDiscountAmount(source.getDiscountAmount());
        newDto.setTotalCost(source.getTotalCost());
        newDto.setTotalProfit(source.getTotalProfit());
        newDto.setProfitRate(source.getProfitRate());
        newDto.setGrandTotal(source.getGrandTotal());
        newDto.setAttachedFilePath(null);

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
        q.setDiscountAmount(dto.getDiscountAmount());
        q.setTotalCost(dto.getTotalCost());
        q.setTotalProfit(dto.getTotalProfit());
        q.setProfitRate(dto.getProfitRate());
        q.setGrandTotal(dto.getGrandTotal());
        q.setAttachedFilePath(dto.getAttachedFilePath());
        q.setIssueDate(dto.getIssueDate());
        q.setRemarks(dto.getRemarks());
        return q;
    }

    /**
     * 新しい見積番号を生成する 形式: Q-yyyyMMdd-HHmmss-RRR-01
     */
    private String generateNewEstimateNo() {
        // 日付と時間の間にハイフンを入れて読みやすく修正
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        int randomNum = ThreadLocalRandom.current().nextInt(100, 1000);

        // Q-日付-ランダム-枝番初期値
        return "Q-" + dateStr + "-" + randomNum + "-01";
    }
}
