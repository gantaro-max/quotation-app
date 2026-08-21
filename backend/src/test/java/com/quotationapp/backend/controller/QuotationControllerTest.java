package com.quotationapp.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quotationapp.backend.dto.QuotationDto;
import com.quotationapp.backend.entity.QuotationItem;
import com.quotationapp.backend.exception.ResourceNotFoundException;
import com.quotationapp.backend.exception.UnauthorizedException;
import com.quotationapp.backend.service.OcrService;
import com.quotationapp.backend.service.QuotationService;

@WebMvcTest(QuotationController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("QuotationControllerテスト")
class QuotationControllerTest {

        @Autowired
        private MockMvc mockMvc;

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
                                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("GET /api/quotations/{id}: 404 Not Found")
        void testFindById_NotFound() throws Exception {
                when(quotationService.findDtoById(999L))
                                .thenThrow(new ResourceNotFoundException("Not Found"));

                mockMvc.perform(get("/api/quotations/999")).andExpect(status().isNotFound());
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
                when(quotationService.update(eq(1L), any(QuotationDto.class), any()))
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
                // ★修正: any() をより具体的にしてマッチングを確実にする
                when(quotationService.update(any(), any(), any()))
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
                MockMultipartFile file = new MockMultipartFile("file", "test.pdf",
                                "application/pdf", "dummy".getBytes());
                QuotationItem item = new QuotationItem();
                item.setItemName("OCR Item");

                when(ocrService.analyzeFile(any())).thenReturn(List.of(item));

                mockMvc.perform(multipart("/api/quotations/ocr").file(file))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));
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
                                .andExpect(status().isOk());
        }
}
