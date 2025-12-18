package com.quotationapp.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quotationapp.backend.dto.QuotationCopyRequest;
import com.quotationapp.backend.dto.QuotationCreateRequest;
import com.quotationapp.backend.dto.QuotationDto;
import com.quotationapp.backend.dto.QuotationUpdateRequest;
import com.quotationapp.backend.entity.Quotation;
import com.quotationapp.backend.service.QuotationService;

@WebMvcTest(QuotationController.class)
@DisplayName("QuotationControllerテスト")
class QuotationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuotationService quotationService;

    @Autowired
    private ObjectMapper objectMapper;

    private Quotation testQuotation;
    private QuotationDto testQuotationDto;

    @BeforeEach
    void setUp() {
        testQuotation = createTestQuotation();
        testQuotationDto = new QuotationDto();
        testQuotationDto.setQuotation(testQuotation);
        testQuotationDto.setQuotationItemList(List.of());
    }

    @Test
    @DisplayName("GET /api/quotations/{id}: 正常系 - 見積詳細を取得")
    void testFindById_Success() throws Exception {
        // Given
        Long id = 1L;
        when(quotationService.findById(id)).thenReturn(testQuotationDto);

        // When & Then
        mockMvc.perform(get("/api/quotations/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.quotation.id").value(1))
                .andExpect(jsonPath("$.data.quotation.estimateNo").value("Q20250101-001"));

        verify(quotationService).findById(id);
    }

    @Test
    @DisplayName("GET /api/quotations/{id}: 見積が見つからない場合、404を返す")
    void testFindById_NotFound() throws Exception {
        // Given
        Long id = 999L;
        when(quotationService.findById(id))
                .thenThrow(new IllegalArgumentException("見積が見つかりません。ID: " + id));

        // When & Then
        mockMvc.perform(get("/api/quotations/{id}", id)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/quotations?createdByUserId={userId}: 正常系 - 作成者IDで見積一覧を取得")
    void testFindByCreatedByUserId_Success() throws Exception {
        // Given
        Integer userId = 1;
        List<Quotation> quotations = List.of(testQuotation);
        when(quotationService.findByCreatedByUserId(userId)).thenReturn(quotations);

        // When & Then
        mockMvc.perform(get("/api/quotations").param("createdByUserId", userId.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));

        verify(quotationService).findByCreatedByUserId(userId);
    }

    @Test
    @DisplayName("GET /api/quotations/search: 正常系 - 見積検索")
    void testSearch_Success() throws Exception {
        // Given
        String customerName = "テスト";
        List<Quotation> quotations = List.of(testQuotation);
        when(quotationService.search(customerName, null, null)).thenReturn(quotations);

        // When & Then
        mockMvc.perform(get("/api/quotations/search").param("customerName", customerName))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        verify(quotationService).search(customerName, null, null);
    }

    @Test
    @DisplayName("POST /api/quotations: 正常系 - 見積を新規作成")
    void testCreate_Success() throws Exception {
        // Given
        QuotationCreateRequest request = new QuotationCreateRequest();
        request.setQuotation(testQuotation);
        request.setItems(List.of());

        when(quotationService.create(any(Quotation.class), anyList())).thenReturn(testQuotationDto);

        // When & Then
        mockMvc.perform(post("/api/quotations").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("見積を作成しました"));

        verify(quotationService).create(any(Quotation.class), anyList());
    }

    @Test
    @DisplayName("PUT /api/quotations/{id}: 正常系 - 見積を更新")
    void testUpdate_Success() throws Exception {
        // Given
        Long id = 1L;
        Integer currentUserId = 1;
        QuotationUpdateRequest request = new QuotationUpdateRequest();
        request.setQuotation(testQuotation);
        request.setItems(List.of());

        when(quotationService.update(any(Quotation.class), anyList(), eq(currentUserId)))
                .thenReturn(testQuotationDto);

        // When & Then
        mockMvc.perform(
                put("/api/quotations/{id}", id).param("currentUserId", currentUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("見積を更新しました"));

        verify(quotationService).update(any(Quotation.class), anyList(), eq(currentUserId));
    }

    @Test
    @DisplayName("PUT /api/quotations/{id}: 権限がない場合、400を返す")
    void testUpdate_Unauthorized() throws Exception {
        // Given
        Long id = 1L;
        Integer currentUserId = 2;
        QuotationUpdateRequest request = new QuotationUpdateRequest();
        request.setQuotation(testQuotation);
        request.setItems(List.of());

        when(quotationService.update(any(Quotation.class), anyList(), eq(currentUserId)))
                .thenThrow(new IllegalArgumentException("見積の編集権限がありません"));

        // When & Then
        mockMvc.perform(
                put("/api/quotations/{id}", id).param("currentUserId", currentUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("DELETE /api/quotations/{id}: 正常系 - 見積を削除")
    void testDelete_Success() throws Exception {
        // Given
        Long id = 1L;
        Integer currentUserId = 1;
        doNothing().when(quotationService).delete(id, currentUserId);

        // When & Then
        mockMvc.perform(
                delete("/api/quotations/{id}", id).param("currentUserId", currentUserId.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("見積を削除しました"));

        verify(quotationService).delete(id, currentUserId);
    }

    @Test
    @DisplayName("POST /api/quotations/{id}/copy: 正常系 - 見積をコピー")
    void testCopy_Success() throws Exception {
        // Given
        Long sourceId = 1L;
        QuotationCopyRequest request = new QuotationCopyRequest();
        request.setNewCreatedByUserId(2);
        request.setNewUserDepartmentName("新部署");

        when(quotationService.copy(sourceId, 2, "新部署")).thenReturn(testQuotationDto);

        // When & Then
        mockMvc.perform(
                post("/api/quotations/{id}/copy", sourceId).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("見積をコピーして新規作成しました"));

        verify(quotationService).copy(sourceId, 2, "新部署");
    }

    @Test
    @DisplayName("POST /api/quotations: バリデーションエラー - 必須項目がnull")
    void testCreate_ValidationError_RequiredFieldsNull() throws Exception {
        // Given
        QuotationCreateRequest request = new QuotationCreateRequest();
        Quotation invalidQuotation = new Quotation();
        // createdByUserIdとcustomerNameがnull
        invalidQuotation.setProjectName("テスト案件");
        request.setQuotation(invalidQuotation);
        request.setItems(List.of());

        // When & Then
        mockMvc.perform(post("/api/quotations").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[*].field").value(
                        Matchers.hasItems("quotation.createdByUserId", "quotation.customerName")));
    }

    @Test
    @DisplayName("POST /api/quotations: バリデーションエラー - 顧客名が空文字")
    void testCreate_ValidationError_CustomerNameBlank() throws Exception {
        // Given
        QuotationCreateRequest request = new QuotationCreateRequest();
        Quotation invalidQuotation = new Quotation();
        invalidQuotation.setCreatedByUserId(1);
        invalidQuotation.setCustomerName(""); // 空文字
        request.setQuotation(invalidQuotation);
        request.setItems(List.of());

        // When & Then
        mockMvc.perform(post("/api/quotations").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[*].field")
                        .value(Matchers.hasItem("quotation.customerName")));
    }

    @Test
    @DisplayName("POST /api/quotations: バリデーションエラー - 明細の行タイプが不正")
    void testCreate_ValidationError_InvalidRowType() throws Exception {
        // Given
        QuotationCreateRequest request = new QuotationCreateRequest();
        request.setQuotation(testQuotation);

        com.quotationapp.backend.entity.QuotationItem invalidItem =
                new com.quotationapp.backend.entity.QuotationItem();
        invalidItem.setRowOrder(1);
        invalidItem.setRowType("invalid"); // 不正な行タイプ
        request.setItems(List.of(invalidItem));

        // When & Then
        mockMvc.perform(post("/api/quotations").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[*].field")
                        .value(Matchers.hasItem("items[0].rowType")));
    }

    @Test
    @DisplayName("PUT /api/quotations/{id}: バリデーションエラー - 必須項目がnull")
    void testUpdate_ValidationError_RequiredFieldsNull() throws Exception {
        // Given
        Long id = 1L;
        Integer currentUserId = 1;
        QuotationUpdateRequest request = new QuotationUpdateRequest();
        Quotation invalidQuotation = new Quotation();
        invalidQuotation.setId(id);
        // createdByUserIdとcustomerNameがnull
        request.setQuotation(invalidQuotation);
        request.setItems(List.of());

        // When & Then
        mockMvc.perform(
                put("/api/quotations/{id}", id).param("currentUserId", currentUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[*].field").value(
                        Matchers.hasItems("quotation.createdByUserId", "quotation.customerName")));
    }

    @Test
    @DisplayName("POST /api/quotations/{id}/copy: バリデーションエラー - 新規作成者IDがnull")
    void testCopy_ValidationError_NewCreatedByUserIdNull() throws Exception {
        // Given
        Long sourceId = 1L;
        QuotationCopyRequest request = new QuotationCopyRequest();
        // newCreatedByUserIdがnull
        request.setNewUserDepartmentName("新部署");

        // When & Then
        mockMvc.perform(
                post("/api/quotations/{id}/copy", sourceId).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[*].field")
                        .value(Matchers.hasItem("newCreatedByUserId")));
    }

    // テストデータ作成ヘルパーメソッド
    private Quotation createTestQuotation() {
        Quotation quotation = new Quotation();
        quotation.setId(1L);
        quotation.setEstimateNo("Q20250101-001");
        quotation.setVersion(1);
        quotation.setIsSubmitted(false);
        quotation.setCreatedByUserId(1);
        quotation.setUserDepartmentName("営業部");
        quotation.setSalesBranchId(1001);
        quotation.setSalesStaffId(10010001);
        quotation.setCustomerId(2001);
        quotation.setCustomerName("テスト顧客");
        quotation.setProjectName("テスト案件");
        quotation.setTotalAmount(new BigDecimal("100000"));
        quotation.setGrandTotal(new BigDecimal("110000"));
        quotation.setIssueDate(LocalDate.of(2025, 1, 1));
        quotation.setRemarks("テスト備考");
        return quotation;
    }
}

