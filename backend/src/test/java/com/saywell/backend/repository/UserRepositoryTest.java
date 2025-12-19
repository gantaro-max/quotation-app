package com.saywell.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate; // テストデータ投入用
import com.saywell.backend.entity.User;

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
}
