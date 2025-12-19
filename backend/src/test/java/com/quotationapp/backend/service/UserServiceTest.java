package com.quotationapp.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.quotationapp.backend.dto.LoginResponse;
import com.quotationapp.backend.entity.User;
import com.quotationapp.backend.exception.ResourceNotFoundException;
import com.quotationapp.backend.exception.UnauthorizedException;
import com.quotationapp.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceテスト")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("login: 成功")
    void testLogin_Success() {
        // Mock設定: 正しいメールとハッシュ済みパスワード
        User user = new User();
        user.setId(1);
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed_password"); // 本来はハッシュ値
        user.setName("テスト太郎");
        user.setDepartmentName("営業部");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // 実行: 入力パスワードが "hashed_password" と一致する場合 (今回は平文比較ロジックのため)
        LoginResponse response = userService.login("test@example.com", "hashed_password");

        // 検証
        assertNotNull(response);
        assertEquals(1, response.getId());
        assertEquals("test@example.com", response.getEmail());
    }

    @Test
    @DisplayName("login: ユーザーが存在しない")
    void testLogin_UserNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.login("unknown@example.com", "pass"));
    }

    @Test
    @DisplayName("login: パスワード不一致")
    void testLogin_PasswordMismatch() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setPasswordHash("correct_pass");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // 間違ったパスワードで実行
        assertThrows(UnauthorizedException.class,
                () -> userService.login("test@example.com", "wrong_pass"));
    }
}
