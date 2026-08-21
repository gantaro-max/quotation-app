package com.quotationapp.backend.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * システム利用者 Entity Table: users
 */
@Data
public class User {
    // システムID
    private Integer id;

    // 利用者名
    private String name;

    // メールアドレス (ログインID)
    private String email;

    // パスワードハッシュ (DBカラム: password_hash)
    private String passwordHash;

    // 所属部署コード (DBカラム: department_code)
    private String departmentCode;

    // 所属営業所ID (DBカラム: branch_id)
    private Integer branchId;

    // 所属部署名 (DBカラム: department_name)
    private String departmentName;

    // 作成日時
    private LocalDateTime createdAt;

    // 更新日時
    private LocalDateTime updatedAt;
}
