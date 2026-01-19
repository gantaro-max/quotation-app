package com.saywell.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.saywell.backend.dto.ApiResponse;
import com.saywell.backend.dto.LoginResponse;
import com.saywell.backend.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * ログインAPI
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        // ビジネスロジック(UserService)に委譲
        LoginResponse response = userService.login(request.getEmail(), request.getPassword());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * リクエスト用DTO (内部クラス)
     */
    @Data
    public static class LoginRequest {
        private String email;
        private String password;
    }
}
