package com.quotationapp.backend.repository;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.quotationapp.backend.entity.Quotation;
import com.quotationapp.backend.entity.QuotationItem;

@Mapper
public interface QuotationRepository {

    /**
     * IDで見積を取得（全ユーザーが参照可能）
     * @param id 見積ID
     * @return 見積エンティティ
     */
    Quotation findById(@Param("id") Long id);

    /**
     * 作成者IDで見積一覧を取得（本人が作成した見積のみ）
     * @param createdByUserId 作成者ID
     * @return 見積一覧
     */
    List<Quotation> findByCreatedByUserId(@Param("createdByUserId") Integer createdByUserId);

    /**
     * 見積検索（得意先名、案件名、見積Noの部分一致、全ユーザーが参照可能）
     * @param customerName 得意先名（部分一致、任意）
     * @param projectName 案件名（部分一致、任意）
     * @param estimateNo 見積No（部分一致、任意）
     * @return 見積一覧
     */
    List<Quotation> search(
        @Param("customerName") String customerName,
        @Param("projectName") String projectName,
        @Param("estimateNo") String estimateNo
    );

    /**
     * 見積を新規保存
     * @param quotation 見積エンティティ
     * @return 挿入件数
     */
    int insert(Quotation quotation);

    /**
     * 見積を更新保存
     * @param quotation 見積エンティティ
     * @return 更新件数
     */
    int update(Quotation quotation);

    /**
     * 見積を削除（作成者本人のみ）
     * @param id 見積ID
     * @param createdByUserId 作成者ID
     * @return 削除件数
     */
    int delete(@Param("id") Long id, @Param("createdByUserId") Integer createdByUserId);

    /**
     * 見積IDで明細一覧を取得
     * @param quotationId 見積ID
     * @return 明細一覧
     */
    List<QuotationItem> findItemsByQuotationId(@Param("quotationId") Long quotationId);

    /**
     * 明細を一括挿入
     * @param items 明細リスト
     * @return 挿入件数
     */
    int insertItems(@Param("items") List<QuotationItem> items);

    /**
     * 見積IDで明細を一括削除
     * @param quotationId 見積ID
     * @return 削除件数
     */
    int deleteItemsByQuotationId(@Param("quotationId") Long quotationId);
}
