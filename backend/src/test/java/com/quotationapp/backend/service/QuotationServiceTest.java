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
import com.quotationapp.backend.exception.ResourceNotFoundException;
import com.quotationapp.backend.exception.UnauthorizedException;
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

    // --- 参照系のテスト ---

    @Test
    @DisplayName("findDtoById: 正常系")
    void testFindDtoById_Success() {
        Long id = 1L;
        when(quotationRepository.findDtoById(id)).thenReturn(Optional.of(testDto));
        QuotationDto result = quotationService.findDtoById(id);
        assertNotNull(result);
        assertEquals(testDto.getEstimateNo(), result.getEstimateNo());
    }

    @Test
    @DisplayName("findDtoById: 存在しない場合エラー")
    void testFindDtoById_NotFound() {
        Long id = 999L;
        when(quotationRepository.findDtoById(id)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> quotationService.findDtoById(id));
    }

    @Test
    @DisplayName("search: 正常系")
    void testSearch_Success() {
        String customerName = "テスト";
        // MockとService呼び出しの両方で引数を5つに修正
        when(quotationRepository.search(customerName, null, null, null, null))
                .thenReturn(List.of(testDto));

        List<QuotationDto> result = quotationService.search(customerName, null, null, null, null);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("findByCreatedByUserId: 正常系")
    void testFindByCreatedByUserId_Success() {
        Integer userId = 1;
        when(quotationRepository.findByCreatedByUserId(userId)).thenReturn(List.of(testDto));
        List<QuotationDto> result = quotationService.findByCreatedByUserId(userId);
        assertEquals(1, result.size());
    }

    // --- 更新系のテスト ---

    @Test
    @DisplayName("create: 正常系")
    void testCreate_Success() {
        doAnswer(invocation -> {
            Quotation q = invocation.getArgument(0);
            q.setId(1L);
            return 1;
        }).when(quotationRepository).insert(any(Quotation.class));

        when(quotationRepository.findDtoById(1L)).thenReturn(Optional.of(testDto));

        QuotationDto result = quotationService.create(testDto);
        assertNotNull(result);
        verify(quotationRepository).insertItems(anyList());
    }

    @Test
    @DisplayName("update: 正常系")
    void testUpdate_Success() {
        Long id = 1L;
        Integer currentUserId = 1;

        when(quotationRepository.findById(id)).thenReturn(testEntity);
        when(quotationRepository.update(any(Quotation.class))).thenReturn(1);
        when(quotationRepository.findDtoById(id)).thenReturn(Optional.of(testDto));

        QuotationDto result = quotationService.update(id, testDto, currentUserId);
        assertNotNull(result);
        verify(quotationRepository).deleteItemsByQuotationId(id);
        verify(quotationRepository).insertItems(anyList());
    }

    @Test
    @DisplayName("update: 権限エラー")
    void testUpdate_Forbidden() {
        Long id = 1L;
        Integer otherUserId = 999;
        when(quotationRepository.findById(id)).thenReturn(testEntity);
        assertThrows(UnauthorizedException.class,
                () -> quotationService.update(id, testDto, otherUserId));
    }

    @Test
    @DisplayName("delete: 正常系")
    void testDelete_Success() {
        Long id = 1L;
        Integer currentUserId = 1;
        when(quotationRepository.findById(id)).thenReturn(testEntity);

        quotationService.delete(id, currentUserId);
        verify(quotationRepository).delete(id, currentUserId);
    }

    @Test
    @DisplayName("copy: 正常系")
    void testCopy_Success() {
        Long sourceId = 1L;
        when(quotationRepository.findDtoById(sourceId)).thenReturn(Optional.of(testDto));

        doAnswer(invocation -> {
            Quotation q = invocation.getArgument(0);
            q.setId(2L); // 新しいID
            return 1;
        }).when(quotationRepository).insert(any(Quotation.class));

        when(quotationRepository.findDtoById(2L)).thenReturn(Optional.of(testDto));

        QuotationDto result = quotationService.copy(sourceId, 2, "新部署");
        assertNotNull(result);
    }

    // --- ヘルパー ---

    private QuotationDto createTestDto() {
        QuotationDto dto = new QuotationDto();
        dto.setId(1L);
        dto.setEstimateNo("Q001");
        dto.setVersion(1);
        dto.setIsSubmitted(false);
        dto.setCreatedByUserId(1);
        dto.setTotalAmount(new BigDecimal("100000"));
        dto.setGrandTotal(new BigDecimal("110000"));
        dto.setIssueDate(LocalDate.now());

        // Items setup
        QuotationItem item = new QuotationItem();
        item.setRowOrder(1);
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(new BigDecimal("1000"));
        dto.setItems(List.of(item));
        return dto;
    }

    private Quotation createTestEntity() {
        Quotation q = new Quotation();
        q.setId(1L);
        q.setCreatedByUserId(1);
        // Entity定義変更への対応
        q.setUserDepartmentName("営業部");
        return q;
    }
}
