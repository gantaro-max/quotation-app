package com.quotationapp.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.quotationapp.backend.dto.LoginResponse;
import com.quotationapp.backend.entity.User;
import com.quotationapp.backend.exception.ResourceNotFoundException;
import com.quotationapp.backend.exception.UnauthorizedException;
import com.quotationapp.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * ログイン処理
     * 
     * @param email メールアドレス
     * @param password 生パスワード
     * @return パスワードを含まない安全なユーザー情報
     */
    @Transactional(readOnly = true)
    public LoginResponse login(String email, String password) {
        // 1. メールアドレスでEntity (パスワードハッシュ付き) を取得
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("ユーザーが見つかりません。email: " + email));

        String inputHashed = hashPassword(password);

        // 2. パスワード照合
        // ※本番環境では BCryptPasswordEncoder 等を使用しますが、現在は単純比較
        if (!user.getPasswordHash().equals(inputHashed)) {
            throw new UnauthorizedException("パスワードが間違っています。");
        }

        // 3. Entity -> DTO (LoginResponse) 変換
        // パスワードはここで除外されます
        return new LoginResponse(user.getId(), user.getName(), user.getEmail(),
                user.getDepartmentName());
    }

    /**
     * SHA-256によるハッシュ化（標準ライブラリ使用） ※後でSpring Securityに移行する際はこのメソッドを削除して置き換えればOKです
     */
    private String hashPassword(String rawPassword) {
        if (rawPassword == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));

            // バイト配列を16進数文字列に変換
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("ハッシュ化アルゴリズムが見つかりません", e);
        }
    }
}
