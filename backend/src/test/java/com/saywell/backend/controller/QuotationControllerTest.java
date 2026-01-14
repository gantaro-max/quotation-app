package com.saywell.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean; // Boot 3.4系の場合
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
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

        @MockitoBean // Spring Boot 3.4以降 (3.3以前なら @MockBean)
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

                mockMvc.perform(post("/api/quotations").contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(testDto)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("PUT /api/quotations/{id}: 権限エラー")
        void testUpdate_Forbidden() throws Exception {
                when(quotationService.update(eq(1L), any(QuotationDto.class), eq(999)))
                                .thenThrow(new UnauthorizedException("Forbidden"));

                mockMvc.perform(put("/api/quotations/1").param("currentUserId", "999")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(testDto)))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /api/quotations/{id}/copy: 正常系")
        void testCopy_Success() throws Exception {
                QuotationCopyRequest req = new QuotationCopyRequest();
                req.setNewCreatedByUserId(2);
                req.setNewUserDepartmentName("新部署");

                when(quotationService.copy(1L, 2, "新部署")).thenReturn(testDto);

                mockMvc.perform(post("/api/quotations/1/copy")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isCreated());
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

                mockMvc.perform(MockMvcRequestBuilders.multipart("/api/quotations/ocr").file(file))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data[0].itemName").value("OCR Item"));
        }

        @Test
        @DisplayName("GET /api/quotations/search: 正常系")
        void testSearch_Success() throws Exception {
                when(quotationService.search(any(), any(), any(), any(), any()))
                                .thenReturn(List.of(testDto));

                mockMvc.perform(get("/api/quotations/search").param("customerName", "test"))
                                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("PUT /api/quotations/{id}: 正常系")
        void testUpdate_Success() throws Exception {
                when(quotationService.update(eq(1L), any(QuotationDto.class), eq(1)))
                                .thenReturn(testDto);

                mockMvc.perform(put("/api/quotations/1").param("currentUserId", "1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(testDto)))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("DELETE /api/quotations/{id}: 正常系")
        void testDelete_Success() throws Exception {
                // deleteはvoidなのでwhen不要(またはdoNothing)

                mockMvc.perform(MockMvcRequestBuilders.delete("/api/quotations/1")
                                .param("currentUserId", "1")).andExpect(status().isOk());
        }
}
