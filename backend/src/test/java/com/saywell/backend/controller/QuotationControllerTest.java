package com.saywell.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
// ★修正: MockBean ではなく MockitoBean を使用
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saywell.backend.dto.QuotationCopyRequest;
import com.saywell.backend.dto.QuotationDto;
import com.saywell.backend.entity.QuotationItem;
import com.saywell.backend.exception.ResourceNotFoundException;
import com.saywell.backend.exception.UnauthorizedException;
import com.saywell.backend.service.OcrService;
import com.saywell.backend.service.QuotationService;

@WebMvcTest(QuotationController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("QuotationControllerテスト")
class QuotationControllerTest {

        @Autowired
        private MockMvc mockMvc;

        // ★修正: Spring Boot 3.4以降の推奨アノテーションに変更
        @MockitoBean
        private QuotationService quotationService;

        @MockitoBean
        private OcrService ocrService;

        @Autowired
        private ObjectMapper objectMapper;

        private QuotationDto testDto;

        @BeforeEach
        void setUp() {
                testDto = new QuotationDto();
                testDto.setId(1L);
                testDto.setEstimateNo("Q001");
                testDto.setVersion(1);
                testDto.setIsSubmitted(false);
                testDto.setCreatedByUserId(1);
                testDto.setCustomerName("テスト顧客");
                testDto.setProjectName("テスト案件");
                testDto.setTotalAmount(BigDecimal.valueOf(1000));
                testDto.setGrandTotal(BigDecimal.valueOf(1100));
                testDto.setItems(List.of());
        }

        @Test
        @DisplayName("GET /api/quotations/{id}: 正常系")
        void testFindById_Success() throws Exception {
                when(quotationService.findDtoById(1L)).thenReturn(testDto);

                mockMvc.perform(get("/api/quotations/1")).andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.estimateNo").value("Q001"));
        }

        @Test
        @DisplayName("GET /api/quotations/{id}: 404 Not Found")
        void testFindById_NotFound() throws Exception {
                when(quotationService.findDtoById(999L))
                                .thenThrow(new ResourceNotFoundException("Not Found"));

                mockMvc.perform(get("/api/quotations/999")).andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.error").value("Not Found"));
        }

        @Test
        @DisplayName("POST /api/quotations: 正常系")
        void testCreate_Success() throws Exception {
                when(quotationService.create(any(QuotationDto.class))).thenReturn(testDto);

                MockMultipartFile jsonPart = new MockMultipartFile("quotation", "",
                                "application/json",
                                objectMapper.writeValueAsString(testDto).getBytes());

                mockMvc.perform(multipart("/api/quotations").file(jsonPart))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("PUT /api/quotations/{id}: 正常系")
        void testUpdate_Success() throws Exception {
                // any()を使って引数条件を緩め、確実にモックを動作させる
                when(quotationService.update(any(), any(QuotationDto.class), any()))
                                .thenReturn(testDto);

                MockMultipartFile jsonPart = new MockMultipartFile("quotation", "",
                                "application/json",
                                objectMapper.writeValueAsString(testDto).getBytes());

                mockMvc.perform(multipart("/api/quotations/1").file(jsonPart).with(request -> {
                        request.setMethod("PUT");
                        return request;
                }).param("currentUserId", "1")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("PUT /api/quotations/{id}: 権限エラー")
        void testUpdate_Forbidden() throws Exception {
                // ★修正: any()を使って引数を問わず例外を投げるように設定
                when(quotationService.update(any(), any(QuotationDto.class), any()))
                                .thenThrow(new UnauthorizedException("Forbidden"));

                MockMultipartFile jsonPart = new MockMultipartFile("quotation", "",
                                "application/json",
                                objectMapper.writeValueAsString(testDto).getBytes());

                mockMvc.perform(multipart("/api/quotations/1").file(jsonPart).with(request -> {
                        request.setMethod("PUT");
                        return request;
                }).param("currentUserId", "999")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /api/quotations/ocr: 正常系")
        void testAnalyzeOcr_Success() throws Exception {
                MockMultipartFile file = new MockMultipartFile("file", "inv.pdf", "application/pdf",
                                "dummy".getBytes());
                QuotationItem item = new QuotationItem();
                item.setItemName("OCR Item");
                item.setQuantity(BigDecimal.ONE);
                item.setUnitPrice(BigDecimal.valueOf(100));

                when(ocrService.analyzeFile(any())).thenReturn(List.of(item));

                mockMvc.perform(multipart("/api/quotations/ocr").file(file))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("POST /api/quotations/{id}/copy: 正常系")
        void testCopy_Success() throws Exception {
                QuotationCopyRequest req = new QuotationCopyRequest();
                req.setNewCreatedByUserId(2);

                // 引数条件を緩める
                when(quotationService.copy(any(), any(), any())).thenReturn(testDto);

                mockMvc.perform(post("/api/quotations/1/copy")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("DELETE /api/quotations/{id}: 正常系")
        void testDelete_Success() throws Exception {
                mockMvc.perform(delete("/api/quotations/1").param("currentUserId", "1"))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/quotations/search: 正常系")
        void testSearch_Success() throws Exception {
                when(quotationService.search(any(), any(), any(), any(), any()))
                                .thenReturn(List.of(testDto));

                mockMvc.perform(get("/api/quotations/search").param("customerName", "test"))
                                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
        }
}
