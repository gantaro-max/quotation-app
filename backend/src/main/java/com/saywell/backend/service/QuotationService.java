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
     * IDで見積DTOを取得 (画面表示・プレビュー用) マスタ情報をJOINした状態で返します。
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
        // 1. DTO -> Entity 変換
        Quotation quotation = toEntity(dto);

        // 初期値設定 (新規)
        quotation.setId(null);
        quotation.setCreatedAt(LocalDateTime.now());
        quotation.setUpdatedAt(LocalDateTime.now());

        // 2. ヘッダー保存
        quotationRepository.insert(quotation);
        Long newId = quotation.getId();

        // 3. 明細保存
        saveItems(newId, dto.getItems());

        // 4. 保存後の完全なデータを再取得して返す
        return findDtoById(newId);
    }

    /**
     * 見積を更新保存 (作成者本人のみ可能)
     */
    @Transactional
    public QuotationDto update(Long id, QuotationDto dto, Integer currentUserId) {
        // 1. 存在チェック & 権限チェック
        Quotation existing = quotationRepository.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("見積が見つかりません。ID: " + id);
        }
        if (!existing.getCreatedByUserId().equals(currentUserId)) {
            throw new UnauthorizedException("権限エラー: 作成者本人以外の見積は編集できません。");
        }

        // 2. DTO -> Entity 変換
        Quotation updateEntity = toEntity(dto);
        updateEntity.setId(id);

        // 変更不可項目を既存データから維持
        updateEntity.setCreatedByUserId(existing.getCreatedByUserId());
        updateEntity.setUserDepartmentName(existing.getUserDepartmentName());
        updateEntity.setCreatedAt(existing.getCreatedAt());
        updateEntity.setUpdatedAt(LocalDateTime.now());

        // 3. ヘッダー更新
        quotationRepository.update(updateEntity);

        // 4. 明細の全入替 (削除 -> 登録)
        quotationRepository.deleteItemsByQuotationId(id);
        saveItems(id, dto.getItems());

        // 5. 最新状態を返す
        return findDtoById(id);
    }

    /**
     * 見積を削除 (作成者本人のみ可能)
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

        // 明細はDBのCASCADE設定(ON DELETE CASCADE)があれば自動で消えるが
        // アプリケーション側でも明示的に削除を実行
        quotationRepository.deleteItemsByQuotationId(id);
        quotationRepository.delete(id, currentUserId);
    }

    /**
     * 見積コピー (参照した見積から新規作成)
     */
    @Transactional
    public QuotationDto copy(Long sourceId, Integer newCreatedByUserId,
            String newUserDepartmentName) {
        // コピー元を取得
        QuotationDto source = findDtoById(sourceId); // なければここでResourceNotFoundExceptionが出る

        // 新しいDTOを作成
        QuotationDto newDto = new QuotationDto();

        // 基本情報のコピー & リセット
        newDto.setEstimateNo(generateNewEstimateNo()); // ※採番
        newDto.setVersion(1);
        newDto.setIsSubmitted(false);
        newDto.setCreatedByUserId(newCreatedByUserId);

        // 案件情報の引継ぎ
        newDto.setSalesBranchId(source.getSalesBranchId());
        newDto.setSalesStaffId(source.getSalesStaffId());
        newDto.setCustomerId(source.getCustomerId());
        newDto.setCustomerName(source.getCustomerName());
        newDto.setProjectName("【コピー】" + source.getProjectName());

        // 日付・備考
        newDto.setIssueDate(null); // 日付はリセット
        newDto.setRemarks(source.getRemarks());

        // 金額情報の引継ぎ
        newDto.setTotalAmount(source.getTotalAmount());
        newDto.setTotalCost(source.getTotalCost());
        newDto.setTotalProfit(source.getTotalProfit());
        newDto.setProfitRate(source.getProfitRate());
        newDto.setGrandTotal(source.getGrandTotal());

        // 明細リストのディープコピー (IDをnullにする)
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

        // 新規作成として保存処理へ委譲
        return create(newDto);
    }

    // =========================================================================
    // Private Helper Methods
    // =========================================================================

    /**
     * 明細リストの保存処理
     */
    private void saveItems(Long quotationId, List<QuotationItem> items) {
        if (items != null && !items.isEmpty()) {
            for (QuotationItem item : items) {
                item.setId(null); // 新規ID発番のためnull化
                item.setQuotationId(quotationId);
            }
            quotationRepository.insertItems(items);
        }
    }

    /**
     * DTO -> Entity 変換
     */
    private Quotation toEntity(QuotationDto dto) {
        Quotation q = new Quotation();
        q.setEstimateNo(dto.getEstimateNo());
        q.setVersion(dto.getVersion());
        q.setIsSubmitted(dto.getIsSubmitted());

        q.setCreatedByUserId(dto.getCreatedByUserId());
        // userDepartmentNameは必要ならDBから取得または引数で渡すなど調整

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

    /**
     * 簡易採番ロジック
     */
    private String generateNewEstimateNo() {
        return "Q" + System.currentTimeMillis();
    }
}
