package com.quotationapp.backend.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.quotationapp.backend.dto.ApiResponse;
import com.quotationapp.backend.dto.QuotationCopyRequest;
import com.quotationapp.backend.dto.QuotationDto;
import com.quotationapp.backend.service.QuotationService;
import lombok.RequiredArgsConstructor;

/**
 * 見積Controller GlobalExceptionHandlerの導入により try-catch を削除しシンプル化しました。
 */
@RestController
@RequestMapping("/api/quotations")
@RequiredArgsConstructor
public class QuotationController {

    private final QuotationService quotationService;

    // =========================================================================
    // 参照系
    // =========================================================================

    /**
     * 見積詳細を取得（全ユーザーが参照可能） GET /api/quotations/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuotationDto>> findById(@PathVariable Long id) {
        // エラー(ResourceNotFoundExceptionなど)はGlobalExceptionHandlerが捕捉します
        QuotationDto dto = quotationService.findDtoById(id);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /**
     * 作成者IDで見積一覧を取得（本人が作成した見積のみ） GET /api/quotations?createdByUserId={userId}
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<QuotationDto>>> findByCreatedByUserId(
            @RequestParam(required = false) Integer createdByUserId) {

        List<QuotationDto> list;
        if (createdByUserId != null) {
            list = quotationService.findByCreatedByUserId(createdByUserId);
        } else {
            // 指定がなければ空リストを返す（または全件返すなどの仕様に合わせて変更可）
            list = List.of();
        }
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    /**
     * 見積検索（全ユーザーが参照可能） GET /api/quotations/search
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<QuotationDto>>> search(
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String projectName,
            @RequestParam(required = false) String estimateNo) {

        List<QuotationDto> list = quotationService.search(customerName, projectName, estimateNo);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    // =========================================================================
    // 更新系
    // =========================================================================

    /**
     * 見積を新規保存 POST /api/quotations
     */
    @PostMapping
    public ResponseEntity<ApiResponse<QuotationDto>> create(
            @Validated @RequestBody QuotationDto dto) {

        QuotationDto resultDto = quotationService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("見積を作成しました", resultDto));
    }

    /**
     * 見積を更新保存（作成者本人のみ） PUT /api/quotations/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<QuotationDto>> update(@PathVariable Long id,
            @Validated @RequestBody QuotationDto dto, @RequestParam Integer currentUserId) {

        // 権限エラー時はServiceからUnauthorizedExceptionが投げられます
        QuotationDto resultDto = quotationService.update(id, dto, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("見積を更新しました", resultDto));
    }

    /**
     * 見積を削除（作成者本人のみ） DELETE /api/quotations/{id}?currentUserId={userId}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
            @RequestParam Integer currentUserId) {

        quotationService.delete(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("見積を削除しました", null));
    }

    /**
     * 見積をコピーして新規作成 POST /api/quotations/{id}/copy
     */
    @PostMapping("/{id}/copy")
    public ResponseEntity<ApiResponse<QuotationDto>> copy(@PathVariable Long id,
            @Validated @RequestBody QuotationCopyRequest request) {

        QuotationDto dto = quotationService.copy(id, request.getNewCreatedByUserId(),
                request.getNewUserDepartmentName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("見積をコピーして新規作成しました", dto));
    }
}
