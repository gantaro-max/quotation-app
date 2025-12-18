package com.saywell.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.saywell.backend.converter.QuotationConverter;
import com.saywell.backend.dto.QuotationDto;
import com.saywell.backend.entity.Quotation;
import com.saywell.backend.entity.QuotationItem;
import com.saywell.backend.repository.QuotationRepository;

/**
 * 見積Service実装クラス
 */
@Service
public class QuotationServiceImpl implements QuotationService {

    private final QuotationRepository quotationRepository;
    private final QuotationConverter quotationConverter;

    public QuotationServiceImpl(
            QuotationRepository quotationRepository,
            QuotationConverter quotationConverter) {
        this.quotationRepository = quotationRepository;
        this.quotationConverter = quotationConverter;
    }

    @Override
    @Transactional(readOnly = true)
    public QuotationDto findById(Long id) {
        Quotation quotation = quotationRepository.findById(id);
        if (quotation == null) {
            throw new IllegalArgumentException("見積が見つかりません。ID: " + id);
        }

        List<QuotationItem> items = quotationRepository.findItemsByQuotationId(id);

        return quotationConverter.toDto(quotation, items);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Quotation> findByCreatedByUserId(Integer createdByUserId) {
        return quotationRepository.findByCreatedByUserId(createdByUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Quotation> search(String customerName, String projectName, String estimateNo) {
        return quotationRepository.search(customerName, projectName, estimateNo);
    }

    @Override
    @Transactional
    public QuotationDto create(Quotation quotation, List<QuotationItem> items) {
        // 新規作成時の初期値設定
        quotationConverter.setDefaultValues(quotation);

        // 見積ヘッダーを保存
        int result = quotationRepository.insert(quotation);
        if (result == 0) {
            throw new RuntimeException("見積の保存に失敗しました。");
        }

        // 明細を保存
        if (items != null && !items.isEmpty()) {
            // 明細に見積IDを設定
            items.forEach(item -> item.setQuotationId(quotation.getId()));
            quotationRepository.insertItems(items);
        }

        // 保存された見積を取得して返す
        return findById(quotation.getId());
    }

    @Override
    @Transactional
    public QuotationDto update(Quotation quotation, List<QuotationItem> items, Integer currentUserId) {
        // 既存の見積を取得
        Quotation existing = quotationRepository.findById(quotation.getId());
        if (existing == null) {
            throw new IllegalArgumentException("見積が見つかりません。ID: " + quotation.getId());
        }

        // 作成者本人かチェック
        if (!existing.getCreatedByUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("見積の編集権限がありません。作成者本人のみ編集可能です。");
        }

        // 更新日時を設定
        quotation.setUpdateAt(LocalDateTime.now());
        // 作成者情報は変更不可
        quotation.setCreatedByUserId(existing.getCreatedByUserId());
        quotation.setUserDepartmentName(existing.getUserDepartmentName());
        quotation.setCreateAt(existing.getCreateAt());

        // 見積ヘッダーを更新
        int result = quotationRepository.update(quotation);
        if (result == 0) {
            throw new RuntimeException("見積の更新に失敗しました。");
        }

        // 既存の明細を削除
        quotationRepository.deleteItemsByQuotationId(quotation.getId());

        // 新しい明細を保存
        if (items != null && !items.isEmpty()) {
            items.forEach(item -> item.setQuotationId(quotation.getId()));
            quotationRepository.insertItems(items);
        }

        // 更新された見積を取得して返す
        return findById(quotation.getId());
    }

    @Override
    @Transactional
    public void delete(Long id, Integer currentUserId) {
        // 既存の見積を取得
        Quotation existing = quotationRepository.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("見積が見つかりません。ID: " + id);
        }

        // 作成者本人かチェック
        if (!existing.getCreatedByUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("見積の削除権限がありません。作成者本人のみ削除可能です。");
        }

        // 明細を削除（CASCADEで自動削除されるが、明示的に削除）
        quotationRepository.deleteItemsByQuotationId(id);

        // 見積を削除
        int result = quotationRepository.delete(id, currentUserId);
        if (result == 0) {
            throw new RuntimeException("見積の削除に失敗しました。");
        }
    }

    @Override
    @Transactional
    public QuotationDto copy(Long sourceId, Integer newCreatedByUserId, String newUserDepartmentName) {
        // コピー元の見積を取得
        QuotationDto sourceDto = findById(sourceId);
        Quotation source = sourceDto.getQuotation();
        List<QuotationItem> sourceItems = sourceDto.getQuotationItemList();

        // 新しい見積を作成
        Quotation newQuotation = quotationConverter.copyQuotation(source, newCreatedByUserId, newUserDepartmentName);

        // 明細をコピー
        List<QuotationItem> newItems = quotationConverter.copyQuotationItems(sourceItems);

        // 新規作成
        return create(newQuotation, newItems);
    }
}

