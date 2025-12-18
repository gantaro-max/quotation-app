package com.saywell.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import; // Importを追加
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saywell.backend.dto.QuotationCopyRequest;
import com.saywell.backend.dto.QuotationDto;
import com.saywell.backend.exception.ResourceNotFoundException;
import com.saywell.backend.exception.UnauthorizedException;
import com.saywell.backend.service.QuotationService;

@WebMvcTest(QuotationController.class)
@Import(GlobalExceptionHandler.class) // ★重要: テスト時にGlobalExceptionHandlerを読み込む
@DisplayName("QuotationControllerテスト")
class QuotationControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private QuotationService quotationService;

        @Autowired
        private ObjectMapper objectMapper;

        private QuotationDto testDto;

        @BeforeEach
        void setUp() {
                testDto = createTestDto();
        }

        // =========================================================================
        // 正常系のテスト (ApiResponseが返る)
        // =========================================================================

        @Test
        @DisplayName("GET /api/quotations/{id}: 正常系")
        void testFindById_Success() throws Exception {
                Long id = 1L;
                when(quotationService.findDtoById(id)).thenReturn(testDto);

                mockMvc.perform(get("/api/quotations/{id}", id)).andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true)) // ApiResponse
                                .andExpect(jsonPath("$.data.estimateNo").value("Q20250101-001"));
        }

        // =========================================================================
        // 異常系のテスト (ErrorResponseが返る)
        // =========================================================================

        @Test
        @DisplayName("GET /api/quotations/{id}: 404 Not Found")
        void testFindById_NotFound() throws Exception {
                Long id = 999L;
                // ResourceNotFoundException を投げるように設定
                when(quotationService.findDtoById(id))
                                .thenThrow(new ResourceNotFoundException("見積が見つかりません"));

                mockMvc.perform(get("/api/quotations/{id}", id)).andExpect(status().isNotFound()) // 404
                                // ErrorResponse の構造をチェック
                                .andExpect(jsonPath("$.error").value("Not Found"))
                                .andExpect(jsonPath("$.message").value("見積が見つかりません"));
        }

        @Test
        @DisplayName("PUT /api/quotations/{id}: 403 Forbidden (権限なし)")
        void testUpdate_Unauthorized() throws Exception {
                Long id = 1L;
                Integer currentUserId = 2;

                // UnauthorizedException を投げるように設定
                when(quotationService.update(eq(id), any(QuotationDto.class), eq(currentUserId)))
                                .thenThrow(new UnauthorizedException("権限エラー"));

                mockMvc.perform(put("/api/quotations/{id}", id)
                                .param("currentUserId", currentUserId.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(testDto)))
                                .andExpect(status().isForbidden()) // 403
                                // ErrorResponse の構造をチェック
                                .andExpect(jsonPath("$.error").value("Forbidden"))
                                .andExpect(jsonPath("$.message").value("権限エラー"));
        }

        @Test
        @DisplayName("POST /api/quotations: 400 Bad Request (バリデーション)")
        void testCreate_ValidationError() throws Exception {
                QuotationDto invalidDto = new QuotationDto();
                // 必須項目がnull

                mockMvc.perform(post("/api/quotations").contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidDto)))
                                .andExpect(status().isBadRequest()) // 400
                                // ErrorResponse の構造 (バリデーション)
                                .andExpect(jsonPath("$.error").value("Validation Failed"))
                                .andExpect(jsonPath("$.fieldErrors").isArray());
        }

        // =========================================================================
        // その他の正常系テスト
        // =========================================================================

        @Test
        @DisplayName("POST /api/quotations: 正常系")
        void testCreate_Success() throws Exception {
                when(quotationService.create(any(QuotationDto.class))).thenReturn(testDto);

                mockMvc.perform(post("/api/quotations").contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(testDto)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("DELETE /api/quotations/{id}: 正常系")
        void testDelete_Success() throws Exception {
                Long id = 1L;
                Integer currentUserId = 1;
                doNothing().when(quotationService).delete(id, currentUserId);

                mockMvc.perform(delete("/api/quotations/{id}", id).param("currentUserId",
                                currentUserId.toString())).andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("POST /api/quotations/{id}/copy: 正常系")
        void testCopy_Success() throws Exception {
                Long sourceId = 1L;
                QuotationCopyRequest request = new QuotationCopyRequest();
                request.setNewCreatedByUserId(2);
                request.setNewUserDepartmentName("新部署");

                when(quotationService.copy(sourceId, 2, "新部署")).thenReturn(testDto);

                mockMvc.perform(post("/api/quotations/{id}/copy", sourceId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.success").value(true));
        }

        // ヘルパー
        private QuotationDto createTestDto() {
                QuotationDto dto = new QuotationDto();
                dto.setId(1L);
                dto.setEstimateNo("Q20250101-001");
                dto.setVersion(1);
                dto.setIsSubmitted(false);
                dto.setCreatedByUserId(1);
                dto.setCustomerName("テスト顧客");
                dto.setTotalAmount(new BigDecimal("100000"));
                dto.setItems(List.of());
                return dto;
        }
}
