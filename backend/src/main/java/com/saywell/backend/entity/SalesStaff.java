package com.saywell.backend.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 営業担当者 Entity Table: sales_staffs
 */
@Data
public class SalesStaff {
    // 社員コード (PK)
    private Integer id;

    // 所属営業所ID (FK)
    private Integer branchId;

    // 担当者名
    private String name;

    // 作成日時
    private LocalDateTime createdAt;

    // 更新日時
    private LocalDateTime updatedAt;
}
