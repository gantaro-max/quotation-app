package com.quotationapp.backend.service;

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

        // 2. パスワード照合
        // ※本番環境では BCryptPasswordEncoder 等を使用しますが、現在は単純比較
        if (!user.getPasswordHash().equals(password)) {
            throw new UnauthorizedException("パスワードが間違っています。");
        }

        // 3. Entity -> DTO (LoginResponse) 変換
        // パスワードはここで除外されます
        return new LoginResponse(user.getId(), user.getName(), user.getEmail(),
                user.getDepartmentName());
    }
}
