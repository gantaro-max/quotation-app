package com.saywell.backend.service;

import java.util.List;
import com.saywell.backend.dto.QuotationDto;
import com.saywell.backend.entity.Quotation;
import com.saywell.backend.entity.QuotationItem;

/**
 * 見積Serviceインターフェース
 */
public interface QuotationService {

    /**
     * 見積詳細を取得（全ユーザーが参照可能）
     * @param id 見積ID
     * @return 見積DTO（見積ヘッダーと明細を含む）
     */
    QuotationDto findById(Long id);

    /**
     * 作成者IDで見積一覧を取得（本人が作成した見積のみ）
     * @param createdByUserId 作成者ID
     * @return 見積一覧
     */
    List<Quotation> findByCreatedByUserId(Integer createdByUserId);

    /**
     * 見積検索（得意先名、案件名、見積Noの部分一致、全ユーザーが参照可能）
     * @param customerName 得意先名（部分一致、任意）
     * @param projectName 案件名（部分一致、任意）
     * @param estimateNo 見積No（部分一致、任意）
     * @return 見積一覧
     */
    List<Quotation> search(String customerName, String projectName, String estimateNo);

    /**
     * 見積を新規保存
     * @param quotation 見積エンティティ
     * @param items 明細リスト
     * @return 保存された見積DTO
     */
    QuotationDto create(Quotation quotation, List<QuotationItem> items);

    /**
     * 見積を更新保存（作成者本人のみ）
     * @param quotation 見積エンティティ
     * @param items 明細リスト
     * @param currentUserId 現在のユーザーID（権限チェック用）
     * @return 更新された見積DTO
     * @throws IllegalArgumentException 作成者本人でない場合
     */
    QuotationDto update(Quotation quotation, List<QuotationItem> items, Integer currentUserId);

    /**
     * 見積を削除（作成者本人のみ）
     * @param id 見積ID
     * @param currentUserId 現在のユーザーID（権限チェック用）
     * @throws IllegalArgumentException 作成者本人でない場合
     */
    void delete(Long id, Integer currentUserId);

    /**
     * 見積をコピーして新規作成（参照した見積から新規の自分の見積を作成）
     * @param sourceId コピー元の見積ID
     * @param newCreatedByUserId 新規作成者のユーザーID
     * @param newUserDepartmentName 新規作成者の部署名
     * @return 新規作成された見積DTO
     */
    QuotationDto copy(Long sourceId, Integer newCreatedByUserId, String newUserDepartmentName);
}




