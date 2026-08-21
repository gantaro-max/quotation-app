package com.quotationapp.backend.repository;

import java.util.List;
import java.util.Optional; // Null対策に追加推奨
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.quotationapp.backend.dto.QuotationDto; // DTOをインポート
import com.quotationapp.backend.entity.Quotation;
import com.quotationapp.backend.entity.QuotationItem;

@Mapper
public interface QuotationRepository {

    // --- Entity操作系 (保存・更新用) ---

    /**
     * IDで見積Entityを取得 (更新時の存在チェック用)
     */
    Quotation findById(@Param("id") Long id);

    /**
     * 見積を新規保存
     */
    int insert(Quotation quotation);

    /**
     * 見積を更新保存
     */
    int update(Quotation quotation);

    /**
     * 見積を削除
     */
    int delete(@Param("id") Long id, @Param("createdByUserId") Integer createdByUserId);

    // --- 明細操作系 ---

    List<QuotationItem> findItemsByQuotationId(@Param("quotationId") Long quotationId);

    int insertItems(@Param("items") List<QuotationItem> items);

    int deleteItemsByQuotationId(@Param("quotationId") Long quotationId);

    // --- DTO参照系 (画面表示・検索用) ---

    /**
     * IDで見積DTOを取得 (画面表示・プレビュー用) マスタ情報をJOINし、明細リストも結合して返す
     */
    Optional<QuotationDto> findDtoById(@Param("id") Long id);

    /**
     * 見積検索 (一覧表示用) ※一覧画面でも担当者名などが必要なため、DTOリストで返すと便利です
     */
    List<QuotationDto> search(@Param("customerName") String customerName,
            @Param("customerCode") String customerCode,
            @Param("salesBranchName") String salesBranchName,
            @Param("projectName") String projectName, @Param("estimateNo") String estimateNo);

    /**
     * 作成者IDで見積一覧を取得
     */
    List<QuotationDto> findByCreatedByUserId(@Param("createdByUserId") Integer createdByUserId);

}
