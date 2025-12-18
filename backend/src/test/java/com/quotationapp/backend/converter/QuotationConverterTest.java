package com.quotationapp.backend.converter;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.quotationapp.backend.dto.QuotationDto;
import com.quotationapp.backend.entity.Quotation;
import com.quotationapp.backend.entity.QuotationItem;

@DisplayName("QuotationConverterテスト")
class QuotationConverterTest {

    private QuotationConverter converter;

    @BeforeEach
    void setUp() {
        converter = new QuotationConverter();
    }

    @Test
    @DisplayName("toDto: 正常系 - 見積と明細リストからDTOに変換")
    void testToDto_Success() {
        // Given
        Quotation quotation = createTestQuotation();
        List<QuotationItem> items = createTestQuotationItems();

        // When
        QuotationDto dto = converter.toDto(quotation, items);

        // Then
        assertNotNull(dto);
        assertEquals(quotation, dto.getQuotation());
        assertEquals(items, dto.getQuotationItemList());
    }

    @Test
    @DisplayName("toDto: 見積がnullの場合、nullを返す")
    void testToDto_QuotationIsNull() {
        // When
        QuotationDto dto = converter.toDto(null, new ArrayList<>());

        // Then
        assertNull(dto);
    }

    @Test
    @DisplayName("toDto: 明細リストがnullの場合、空のリストを設定")
    void testToDto_ItemsIsNull() {
        // Given
        Quotation quotation = createTestQuotation();

        // When
        QuotationDto dto = converter.toDto(quotation, null);

        // Then
        assertNotNull(dto);
        assertNotNull(dto.getQuotationItemList());
        assertTrue(dto.getQuotationItemList().isEmpty());
    }

    @Test
    @DisplayName("setDefaultValues: 初期値が設定される")
    void testSetDefaultValues_Success() {
        // Given
        Quotation quotation = new Quotation();
        quotation.setCreatedByUserId(1);

        // When
        converter.setDefaultValues(quotation);

        // Then
        assertEquals(1, quotation.getVersion());
        assertFalse(quotation.getIsSubmitted());
        assertNotNull(quotation.getIssueDate());
        assertNotNull(quotation.getCreateAt());
        assertNotNull(quotation.getUpdateAt());
    }

    @Test
    @DisplayName("setDefaultValues: 既に値が設定されている場合は変更しない")
    void testSetDefaultValues_ExistingValues() {
        // Given
        Quotation quotation = new Quotation();
        quotation.setVersion(2);
        quotation.setIsSubmitted(true);
        LocalDate issueDate = LocalDate.of(2025, 1, 1);
        quotation.setIssueDate(issueDate);

        // When
        converter.setDefaultValues(quotation);

        // Then
        assertEquals(2, quotation.getVersion());
        assertTrue(quotation.getIsSubmitted());
        assertEquals(issueDate, quotation.getIssueDate());
    }

    @Test
    @DisplayName("setDefaultValues: quotationがnullの場合は何もしない")
    void testSetDefaultValues_QuotationIsNull() {
        // When & Then
        assertDoesNotThrow(() -> converter.setDefaultValues(null));
    }

    @Test
    @DisplayName("copyQuotation: 見積をコピーして新しい見積を作成")
    void testCopyQuotation_Success() {
        // Given
        Quotation source = createTestQuotation();
        Integer newUserId = 2;
        String newDepartmentName = "新部署";

        // When
        Quotation copied = converter.copyQuotation(source, newUserId, newDepartmentName);

        // Then
        assertNotNull(copied);
        assertNull(copied.getEstimateNo());
        assertEquals(1, copied.getVersion());
        assertFalse(copied.getIsSubmitted());
        assertEquals(newUserId, copied.getCreatedByUserId());
        assertEquals(newDepartmentName, copied.getUserDepartmentName());
        assertEquals(source.getSalesBranchId(), copied.getSalesBranchId());
        assertEquals(source.getCustomerName(), copied.getCustomerName());
        assertEquals(source.getProjectName(), copied.getProjectName());
        assertNotNull(copied.getIssueDate());
        assertNull(copied.getAttachedFilePath());
    }

    @Test
    @DisplayName("copyQuotation: sourceがnullの場合はnullを返す")
    void testCopyQuotation_SourceIsNull() {
        // When
        Quotation copied = converter.copyQuotation(null, 1, "部署");

        // Then
        assertNull(copied);
    }

    @Test
    @DisplayName("copyQuotationItem: 明細をコピー")
    void testCopyQuotationItem_Success() {
        // Given
        QuotationItem source = createTestQuotationItem();

        // When
        QuotationItem copied = converter.copyQuotationItem(source);

        // Then
        assertNotNull(copied);
        assertEquals(source.getRowOrder(), copied.getRowOrder());
        assertEquals(source.getRowType(), copied.getRowType());
        assertEquals(source.getItemCode(), copied.getItemCode());
        assertEquals(source.getItemName(), copied.getItemName());
        assertEquals(source.getQuantity(), copied.getQuantity());
        assertNull(copied.getQuotationId()); // quotationIdは設定されない
    }

    @Test
    @DisplayName("copyQuotationItems: 明細リストをコピー")
    void testCopyQuotationItems_Success() {
        // Given
        List<QuotationItem> sourceItems = createTestQuotationItems();

        // When
        List<QuotationItem> copiedItems = converter.copyQuotationItems(sourceItems);

        // Then
        assertNotNull(copiedItems);
        assertEquals(sourceItems.size(), copiedItems.size());
        for (int i = 0; i < sourceItems.size(); i++) {
            QuotationItem source = sourceItems.get(i);
            QuotationItem copied = copiedItems.get(i);
            assertEquals(source.getRowOrder(), copied.getRowOrder());
            assertEquals(source.getItemName(), copied.getItemName());
        }
    }

    @Test
    @DisplayName("copyQuotationItems: nullの場合は空リストを返す")
    void testCopyQuotationItems_Null() {
        // When
        List<QuotationItem> copiedItems = converter.copyQuotationItems(null);

        // Then
        assertNotNull(copiedItems);
        assertTrue(copiedItems.isEmpty());
    }

    @Test
    @DisplayName("copyQuotationItems: 空リストの場合は空リストを返す")
    void testCopyQuotationItems_Empty() {
        // When
        List<QuotationItem> copiedItems = converter.copyQuotationItems(new ArrayList<>());

        // Then
        assertNotNull(copiedItems);
        assertTrue(copiedItems.isEmpty());
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

    private List<QuotationItem> createTestQuotationItems() {
        List<QuotationItem> items = new ArrayList<>();
        QuotationItem item1 = createTestQuotationItem();
        item1.setRowOrder(1);
        items.add(item1);
        return items;
    }

    private QuotationItem createTestQuotationItem() {
        QuotationItem item = new QuotationItem();
        item.setId(1L);
        item.setQuotationId(1L);
        item.setRowOrder(1);
        item.setRowType("normal");
        item.setItemCode("123456789");
        item.setItemName("テスト商品");
        item.setManufacturer("テストメーカー");
        item.setQuantity(new BigDecimal("10"));
        item.setCostPrice(new BigDecimal("8000"));
        item.setUnitPrice(new BigDecimal("10000"));
        return item;
    }
}

