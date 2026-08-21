package com.quotationapp.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate; // テストデータ投入用
import com.quotationapp.backend.entity.User;

@MybatisTest // MyBatisのテスト専用アノテーション
@DisplayName("UserRepositoryのテスト")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("findByEmail: 正しくマッピングされること")
    void testFindByEmail() {
        // 1. テストデータの準備 (直接SQLで投入)
        jdbcTemplate
                .update("""
                            INSERT INTO users (name, email, password_hash, department_name, created_at, updated_at)
                            VALUES ('テスト太郎', 'test@example.com', 'hashed_pw', '営業部', NOW(), NOW())
                        """);

        // 2. 実行
        Optional<User> result = userRepository.findByEmail("test@example.com");

        // 3. 検証
        assertThat(result).isPresent();
        User user = result.get();
        assertThat(user.getName()).isEqualTo("テスト太郎");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        // 重要: スネークケース(password_hash) -> キャメルケース(passwordHash) のマッピング確認
        assertThat(user.getPasswordHash()).isEqualTo("hashed_pw");
        assertThat(user.getDepartmentName()).isEqualTo("営業部");
    }

    @Test
    @DisplayName("findByEmail: ユーザーごとのbranch_idをNULLも含めてマッピングする")
    void findByEmailMapsDifferentBranchIdsIncludingNull() {
        jdbcTemplate.update("INSERT INTO branches (id, name) VALUES (1001, '第一営業所')");
        jdbcTemplate.update("INSERT INTO branches (id, name) VALUES (1002, '第二営業所')");
        jdbcTemplate.update("""
                INSERT INTO users (name, email, password_hash, branch_id)
                VALUES ('第一ユーザー', 'first@example.com', 'hash', 1001)
                """);
        jdbcTemplate.update("""
                INSERT INTO users (name, email, password_hash, branch_id)
                VALUES ('第二ユーザー', 'second@example.com', 'hash', 1002)
                """);
        jdbcTemplate.update("""
                INSERT INTO users (name, email, password_hash, branch_id)
                VALUES ('未設定ユーザー', 'null@example.com', 'hash', NULL)
                """);

        Optional<User> first = userRepository.findByEmail("first@example.com");
        Optional<User> second = userRepository.findByEmail("second@example.com");
        Optional<User> withoutBranch = userRepository.findByEmail("null@example.com");

        assertThat(first).get().extracting(User::getBranchId).isEqualTo(1001);
        assertThat(second).get().extracting(User::getBranchId).isEqualTo(1002);
        assertThat(withoutBranch).get().extracting(User::getBranchId).isNull();
    }
}
