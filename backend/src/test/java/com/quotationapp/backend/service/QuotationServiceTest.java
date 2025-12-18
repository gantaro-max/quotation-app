package com.quotationapp.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.quotationapp.backend.dto.QuotationDto;
import com.quotationapp.backend.entity.Quotation;
import com.quotationapp.backend.entity.QuotationItem;
import com.quotationapp.backend.exception.ResourceNotFoundException; // カスタム例外
import com.quotationapp.backend.exception.UnauthorizedException; // カスタム例外
import com.quotationapp.backend.repository.QuotationRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuotationServiceテスト")
class QuotationServiceTest {

    @Mock
    private QuotationRepository quotationRepository;

    @InjectMocks
    private QuotationService quotationService;

    private QuotationDto testDto;
    private Quotation testEntity;

    @BeforeEach
    void setUp() {
        testDto = createTestDto();
        testEntity = createTestEntity();
    }

    // =========================================================================
    // 参照系のテスト
    // =========================================================================

    @Test
    @DisplayName("findDtoById: 正常系 - DTOを取得")
    void testFindDtoById_Success() {
        Long id = 1L;
        when(quotationRepository.findDtoById(id)).thenReturn(Optional.of(testDto));

        QuotationDto result = quotationService.findDtoById(id);

        assertNotNull(result);
        assertEquals(testDto.getEstimateNo(), result.getEstimateNo());
        verify(quotationRepository).findDtoById(id);
    }

    @Test
    @DisplayName("findDtoById: 存在しない場合 - ResourceNotFoundException")
    void testFindDtoById_NotFound() {
        Long id = 999L;
        when(quotationRepository.findDtoById(id)).thenReturn(Optional.empty());

        // 変更点: IllegalArgumentException -> ResourceNotFoundException
        assertThrows(ResourceNotFoundException.class, () -> quotationService.findDtoById(id));
    }

    @Test
    @DisplayName("search: 正常系 - 検索結果リストを返す")
    void testSearch_Success() {
        String customerName = "テスト";
        List<QuotationDto> expectedList = List.of(testDto);
        when(quotationRepository.search(customerName, null, null)).thenReturn(expectedList);

        List<QuotationDto> result = quotationService.search(customerName, null, null);

        assertEquals(1, result.size());
        verify(quotationRepository).search(customerName, null, null);
    }

    @Test
    @DisplayName("findByCreatedByUserId: 正常系 - 作成者IDで一覧取得")
    void testFindByCreatedByUserId_Success() {
        Integer userId = 1;
        List<QuotationDto> expectedList = List.of(testDto);
        when(quotationRepository.findByCreatedByUserId(userId)).thenReturn(expectedList);

        List<QuotationDto> result = quotationService.findByCreatedByUserId(userId);

        assertEquals(1, result.size());
        verify(quotationRepository).findByCreatedByUserId(userId);
    }

    // =========================================================================
    // 更新系のテスト
    // =========================================================================

    @Test
    @DisplayName("create: 正常系 - 新規作成")
    void testCreate_Success() {
        doAnswer(invocation -> {
            Quotation q = invocation.getArgument(0);
            q.setId(1L);
            return 1;
        }).when(quotationRepository).insert(any(Quotation.class));

        when(quotationRepository.insertItems(anyList())).thenReturn(1);
        when(quotationRepository.findDtoById(1L)).thenReturn(Optional.of(testDto));

        QuotationDto result = quotationService.create(testDto);

        assertNotNull(result);
        verify(quotationRepository).insert(any(Quotation.class));
        verify(quotationRepository).insertItems(anyList());
    }

    @Test
    @DisplayName("update: 正常系 - 更新成功")
    void testUpdate_Success() {
        Long id = 1L;
        Integer currentUserId = 1;

        when(quotationRepository.findById(id)).thenReturn(testEntity);
        when(quotationRepository.update(any(Quotation.class))).thenReturn(1);
        when(quotationRepository.deleteItemsByQuotationId(id)).thenReturn(1);
        when(quotationRepository.insertItems(anyList())).thenReturn(1);
        when(quotationRepository.findDtoById(id)).thenReturn(Optional.of(testDto));

        QuotationDto result = quotationService.update(id, testDto, currentUserId);

        assertNotNull(result);
        verify(quotationRepository).update(any(Quotation.class));
    }

    @Test
    @DisplayName("update: 存在しない場合 - ResourceNotFoundException")
    void testUpdate_NotFound() {
        Long id = 999L;
        Integer currentUserId = 1;
        when(quotationRepository.findById(id)).thenReturn(null);

        // 変更点: ResourceNotFoundException を期待
        assertThrows(ResourceNotFoundException.class,
                () -> quotationService.update(id, testDto, currentUserId));
    }

    @Test
    @DisplayName("update: 権限エラー - UnauthorizedException")
    void testUpdate_Forbidden() {
        Long id = 1L;
        Integer currentUserId = 999; // 他人

        when(quotationRepository.findById(id)).thenReturn(testEntity);

        // 変更点: SecurityException -> UnauthorizedException
        assertThrows(UnauthorizedException.class,
                () -> quotationService.update(id, testDto, currentUserId));
    }

    @Test
    @DisplayName("delete: 正常系 - 削除成功")
    void testDelete_Success() {
        Long id = 1L;
        Integer currentUserId = 1;

        when(quotationRepository.findById(id)).thenReturn(testEntity);
        when(quotationRepository.delete(id, currentUserId)).thenReturn(1);

        quotationService.delete(id, currentUserId);

        verify(quotationRepository).deleteItemsByQuotationId(id);
        verify(quotationRepository).delete(id, currentUserId);
    }

    @Test
    @DisplayName("copy: 正常系 - コピー作成")
    void testCopy_Success() {
        Long sourceId = 1L;
        Integer newUserId = 2;
        String newDept = "新営業部";

        when(quotationRepository.findDtoById(sourceId)).thenReturn(Optional.of(testDto));

        doAnswer(invocation -> {
            Quotation q = invocation.getArgument(0);
            q.setId(2L);
            return 1;
        }).when(quotationRepository).insert(any(Quotation.class));

        when(quotationRepository.findDtoById(2L)).thenReturn(Optional.of(testDto));

        QuotationDto result = quotationService.copy(sourceId, newUserId, newDept);

        assertNotNull(result);
        verify(quotationRepository).insert(any(Quotation.class));
    }

    // =========================================================================
    // ヘルパー
    // =========================================================================

    private QuotationDto createTestDto() {
        QuotationDto dto = new QuotationDto();
        dto.setId(1L);
        dto.setEstimateNo("Q20250101-001");
        dto.setVersion(1);
        dto.setIsSubmitted(false);
        dto.setCreatedByUserId(1);
        dto.setCustomerName("テスト顧客");
        dto.setProjectName("テスト案件");
        dto.setTotalAmount(new BigDecimal("100000"));
        dto.setGrandTotal(new BigDecimal("110000"));
        dto.setIssueDate(LocalDate.of(2025, 1, 1));

        QuotationItem item = new QuotationItem();
        item.setRowOrder(1);
        item.setItemCode("123456789");
        item.setQuantity(BigDecimal.TEN);

        List<QuotationItem> items = new ArrayList<>();
        items.add(item);
        dto.setItems(items);
        return dto;
    }

    private Quotation createTestEntity() {
        Quotation q = new Quotation();
        q.setId(1L);
        q.setCreatedByUserId(1);
        return q;
    }
}
