package com.saywell.backend.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 得意先マスタ Entity Table: customers
 */
@Data
public class Customer {
    // 顧客コード (PK)
    private Integer id;

    // 顧客名
    private String name;

    // 検索用フリガナ (DBカラム: kana)
    private String kana;

    // 管轄営業所ID (FK)
    private Integer branchId;

    // 担当営業ID (FK)
    private Integer salesStaffId;

    // 住所 (DBカラム: address)
    private String address;

    // 作成日時
    private LocalDateTime createdAt;

    // 更新日時
    private LocalDateTime updatedAt;
}
