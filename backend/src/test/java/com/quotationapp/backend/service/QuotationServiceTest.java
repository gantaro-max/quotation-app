package com.quotationapp.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.quotationapp.backend.converter.QuotationConverter;
import com.quotationapp.backend.dto.QuotationDto;
import com.quotationapp.backend.entity.Quotation;
import com.quotationapp.backend.entity.QuotationItem;
import com.quotationapp.backend.repository.QuotationRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuotationServiceテスト")
class QuotationServiceTest {

    @Mock
    private QuotationRepository quotationRepository;

    @Mock
    private QuotationConverter quotationConverter;

    @InjectMocks
    private QuotationServiceImpl quotationService;

    private Quotation testQuotation;
    private List<QuotationItem> testItems;

    @BeforeEach
    void setUp() {
        testQuotation = createTestQuotation();
        testItems = createTestQuotationItems();
    }

    @Test
    @DisplayName("findById: 正常系 - 見積詳細を取得")
    void testFindById_Success() {
        // Given
        Long id = 1L;
        QuotationDto expectedDto = new QuotationDto();
        expectedDto.setQuotation(testQuotation);
        expectedDto.setQuotationItemList(testItems);

        when(quotationRepository.findById(id)).thenReturn(testQuotation);
        when(quotationRepository.findItemsByQuotationId(id)).thenReturn(testItems);
        when(quotationConverter.toDto(testQuotation, testItems)).thenReturn(expectedDto);

        // When
        QuotationDto result = quotationService.findById(id);

        // Then
        assertNotNull(result);
        assertEquals(expectedDto, result);
        verify(quotationRepository).findById(id);
        verify(quotationRepository).findItemsByQuotationId(id);
    }

    @Test
    @DisplayName("findById: 見積が見つからない場合、例外をスロー")
    void testFindById_NotFound() {
        // Given
        Long id = 999L;
        when(quotationRepository.findById(id)).thenReturn(null);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> quotationService.findById(id));
    }

    @Test
    @DisplayName("findByCreatedByUserId: 正常系 - 作成者IDで見積一覧を取得")
    void testFindByCreatedByUserId_Success() {
        // Given
        Integer userId = 1;
        List<Quotation> expectedList = List.of(testQuotation);
        when(quotationRepository.findByCreatedByUserId(userId)).thenReturn(expectedList);

        // When
        List<Quotation> result = quotationService.findByCreatedByUserId(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(quotationRepository).findByCreatedByUserId(userId);
    }

    @Test
    @DisplayName("search: 正常系 - 見積検索")
    void testSearch_Success() {
        // Given
        String customerName = "テスト";
        List<Quotation> expectedList = List.of(testQuotation);
        when(quotationRepository.search(customerName, null, null)).thenReturn(expectedList);

        // When
        List<Quotation> result = quotationService.search(customerName, null, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(quotationRepository).search(customerName, null, null);
    }

    @Test
    @DisplayName("create: 正常系 - 見積を新規作成")
    void testCreate_Success() {
        // Given
        QuotationDto expectedDto = new QuotationDto();
        expectedDto.setQuotation(testQuotation);
        expectedDto.setQuotationItemList(testItems);

        doNothing().when(quotationConverter).setDefaultValues(any(Quotation.class));
        when(quotationRepository.insert(any(Quotation.class))).thenAnswer(invocation -> {
            Quotation q = invocation.getArgument(0);
            q.setId(1L);
            return 1;
        });
        when(quotationRepository.findById(1L)).thenReturn(testQuotation);
        when(quotationRepository.findItemsByQuotationId(1L)).thenReturn(testItems);
        when(quotationConverter.toDto(testQuotation, testItems)).thenReturn(expectedDto);

        // When
        QuotationDto result = quotationService.create(testQuotation, testItems);

        // Then
        assertNotNull(result);
        verify(quotationConverter).setDefaultValues(testQuotation);
        verify(quotationRepository).insert(testQuotation);
        verify(quotationRepository).insertItems(testItems);
    }

    @Test
    @DisplayName("update: 正常系 - 見積を更新（作成者本人）")
    void testUpdate_Success() {
        // Given
        Long id = 1L;
        Integer currentUserId = 1;
        testQuotation.setId(id);
        testQuotation.setCreatedByUserId(currentUserId);

        QuotationDto expectedDto = new QuotationDto();
        expectedDto.setQuotation(testQuotation);

        when(quotationRepository.findById(id)).thenReturn(testQuotation);
        when(quotationRepository.update(any(Quotation.class))).thenReturn(1);
        when(quotationRepository.findById(id)).thenReturn(testQuotation);
        when(quotationRepository.findItemsByQuotationId(id)).thenReturn(testItems);
        when(quotationConverter.toDto(testQuotation, testItems)).thenReturn(expectedDto);

        // When
        QuotationDto result = quotationService.update(testQuotation, testItems, currentUserId);

        // Then
        assertNotNull(result);
        verify(quotationRepository).update(testQuotation);
        verify(quotationRepository).deleteItemsByQuotationId(id);
        verify(quotationRepository).insertItems(testItems);
    }

    @Test
    @DisplayName("update: 作成者本人でない場合、例外をスロー")
    void testUpdate_Unauthorized() {
        // Given
        Long id = 1L;
        Integer currentUserId = 2; // 異なるユーザー
        testQuotation.setId(id);
        testQuotation.setCreatedByUserId(1);

        when(quotationRepository.findById(id)).thenReturn(testQuotation);

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> quotationService.update(testQuotation, testItems, currentUserId));
    }

    @Test
    @DisplayName("delete: 正常系 - 見積を削除（作成者本人）")
    void testDelete_Success() {
        // Given
        Long id = 1L;
        Integer currentUserId = 1;
        testQuotation.setId(id);
        testQuotation.setCreatedByUserId(currentUserId);

        when(quotationRepository.findById(id)).thenReturn(testQuotation);
        when(quotationRepository.delete(id, currentUserId)).thenReturn(1);

        // When
        quotationService.delete(id, currentUserId);

        // Then
        verify(quotationRepository).deleteItemsByQuotationId(id);
        verify(quotationRepository).delete(id, currentUserId);
    }

    @Test
    @DisplayName("delete: 作成者本人でない場合、例外をスロー")
    void testDelete_Unauthorized() {
        // Given
        Long id = 1L;
        Integer currentUserId = 2; // 異なるユーザー
        testQuotation.setId(id);
        testQuotation.setCreatedByUserId(1);

        when(quotationRepository.findById(id)).thenReturn(testQuotation);

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> quotationService.delete(id, currentUserId));
    }

    @Test
    @DisplayName("copy: 正常系 - 見積をコピーして新規作成")
    void testCopy_Success() {
        // Given
        Long sourceId = 1L;
        Integer newUserId = 2;
        String newDepartmentName = "新部署";

        QuotationDto sourceDto = new QuotationDto();
        sourceDto.setQuotation(testQuotation);
        sourceDto.setQuotationItemList(testItems);

        Quotation copiedQuotation = new Quotation();
        copiedQuotation.setId(2L);
        List<QuotationItem> copiedItems = new ArrayList<>();

        QuotationDto expectedDto = new QuotationDto();
        expectedDto.setQuotation(copiedQuotation);

        when(quotationRepository.findById(sourceId)).thenReturn(testQuotation);
        when(quotationRepository.findItemsByQuotationId(sourceId)).thenReturn(testItems);
        when(quotationConverter.toDto(testQuotation, testItems)).thenReturn(sourceDto);
        when(quotationConverter.copyQuotation(testQuotation, newUserId, newDepartmentName))
                .thenReturn(copiedQuotation);
        when(quotationConverter.copyQuotationItems(testItems)).thenReturn(copiedItems);
        doNothing().when(quotationConverter).setDefaultValues(any(Quotation.class));
        when(quotationRepository.insert(any(Quotation.class))).thenAnswer(invocation -> {
            Quotation q = invocation.getArgument(0);
            q.setId(2L);
            return 1;
        });
        when(quotationRepository.findById(2L)).thenReturn(copiedQuotation);
        when(quotationRepository.findItemsByQuotationId(2L)).thenReturn(copiedItems);
        when(quotationConverter.toDto(copiedQuotation, copiedItems)).thenReturn(expectedDto);

        // When
        QuotationDto result = quotationService.copy(sourceId, newUserId, newDepartmentName);

        // Then
        assertNotNull(result);
        verify(quotationConverter).copyQuotation(testQuotation, newUserId, newDepartmentName);
        verify(quotationConverter).copyQuotationItems(testItems);
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
        QuotationItem item = new QuotationItem();
        item.setId(1L);
        item.setQuotationId(1L);
        item.setRowOrder(1);
        item.setRowType("normal");
        item.setItemCode("123456789");
        item.setItemName("テスト商品");
        item.setQuantity(new BigDecimal("10"));
        item.setCostPrice(new BigDecimal("8000"));
        item.setUnitPrice(new BigDecimal("10000"));
        return List.of(item);
    }
}




