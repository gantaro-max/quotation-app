package com.quotationapp.backend.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quotationapp.backend.dto.LoginResponse;
import com.quotationapp.backend.exception.UnauthorizedException;
import com.quotationapp.backend.service.UserService;

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("UserControllerテスト")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/login: 成功")
    void testLogin_Success() throws Exception {
        // リクエスト
        UserController.LoginRequest request = new UserController.LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password");

        // Mock返却値
        LoginResponse response = new LoginResponse(1, "User", "test@example.com", "Dept", 1001);
        when(userService.login("test@example.com", "password")).thenReturn(response);

        mockMvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.branchId").value(1001));
    }

    @Test
    @DisplayName("POST /api/login: 失敗(403 Forbidden)")
    void testLogin_Fail() throws Exception {
        UserController.LoginRequest request = new UserController.LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrong");

        when(userService.login(anyString(), anyString()))
                .thenThrow(new UnauthorizedException("パスワードが違います"));

        mockMvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden()) // UnauthorizedExceptionは通常403/401
                .andExpect(jsonPath("$.error").exists());
    }
}
