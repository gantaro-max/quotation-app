package com.saywell.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.saywell.backend.dto.LoginResponse;
import com.saywell.backend.entity.User;
import com.saywell.backend.exception.ResourceNotFoundException;
import com.saywell.backend.exception.UnauthorizedException;
import com.saywell.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceテスト")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    // "hashed_password" という文字列の SHA-256 ハッシュ値
    private static final String VALID_HASH =
            "b2867617492e26c338ab49f72afabc984d798b59755a27e312b953716ae964d7";

    @Test
    @DisplayName("login: 成功")
    void testLogin_Success() {
        // 準備: DBに存在するユーザー (パスワードはハッシュ化済み)
        User user = new User();
        user.setId(1);
        user.setEmail("test@example.com");
        user.setPasswordHash(VALID_HASH); // DBには正しいハッシュ値が入っている
        user.setName("テスト太郎");
        user.setDepartmentName("営業部");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // 実行: 正しい平文パスワード "hashed_password" を入力
        // (内部でハッシュ化され、DBのVALID_HASHと一致するため成功する)
        LoginResponse response = userService.login("test@example.com", "hashed_password");

        // 検証
        assertNotNull(response);
        assertEquals(1, response.getId());
        assertEquals("test@example.com", response.getEmail());
    }

    @Test
    @DisplayName("login: 所属営業所が設定されたユーザーのbranchIdを返す")
    void loginReturnsConfiguredBranchId() {
        User user = new User();
        user.setId(1);
        user.setEmail("branch-user@example.com");
        user.setPasswordHash(VALID_HASH);
        user.setName("営業所ユーザー");
        user.setDepartmentName("営業部");
        user.setBranchId(1001);

        when(userRepository.findByEmail("branch-user@example.com"))
                .thenReturn(Optional.of(user));

        LoginResponse response =
                userService.login("branch-user@example.com", "hashed_password");

        assertEquals(1001, response.getBranchId());
    }

    @Test
    @DisplayName("login: 所属営業所が未設定ならbranchIdをnullで返す")
    void loginReturnsNullWhenBranchIdIsNotConfigured() {
        User user = new User();
        user.setId(2);
        user.setEmail("no-branch@example.com");
        user.setPasswordHash(VALID_HASH);
        user.setName("営業所未設定ユーザー");
        user.setDepartmentName("営業部");
        user.setBranchId(null);

        when(userRepository.findByEmail("no-branch@example.com"))
                .thenReturn(Optional.of(user));

        LoginResponse response =
                userService.login("no-branch@example.com", "hashed_password");

        assertNull(response.getBranchId());
    }

    @Test
    @DisplayName("login: ユーザーが存在しない")
    void testLogin_UserNotFound() {
        // 準備: 該当メールアドレスのユーザーはいない
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        // 実行 & 検証: ResourceNotFoundException が発生すること
        assertThrows(ResourceNotFoundException.class,
                () -> userService.login("unknown@example.com", "any_password"));
    }

    @Test
    @DisplayName("login: パスワード不一致")
    void testLogin_PasswordMismatch() {
        // 準備: DBには正しいユーザーがいる
        User user = new User();
        user.setEmail("test@example.com");
        user.setPasswordHash(VALID_HASH); // 正しいハッシュ値

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // 実行 & 検証: 間違った平文パスワード "wrong_password" を入力 -> UnauthorizedException
        assertThrows(UnauthorizedException.class,
                () -> userService.login("test@example.com", "wrong_password"));
    }
}
