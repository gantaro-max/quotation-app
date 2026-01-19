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

    // 所属部署名 (DBカラム: department_name)
    // ※ 以前の branchId は sales_staffs テーブル側の持ち物なので、ここには含めません
    private String departmentName;

    // 作成日時
    private LocalDateTime createdAt;

    // 更新日時
    private LocalDateTime updatedAt;
}
