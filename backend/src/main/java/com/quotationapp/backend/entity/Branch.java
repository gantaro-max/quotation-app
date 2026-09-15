package com.quotationapp.backend.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 営業所マスタ Entity Table: branches
 */
@Data
public class Branch {
    // 拠点コード
    private Integer id;

    // 営業所名
    private String name;

    // 表示順 (DBカラム: sort_no)
    private Integer sortNo;

    // 住所
    private String address;

    // 電話番号
    private String phone;

    // 作成日時
    private LocalDateTime createdAt;

    // 更新日時
    private LocalDateTime updatedAt;
}
