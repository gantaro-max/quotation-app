package com.saywell.backend.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.saywell.backend.dto.ApiResponse;
import com.saywell.backend.dto.QuotationCopyRequest;
import com.saywell.backend.dto.QuotationCreateRequest;
import com.saywell.backend.dto.QuotationDto;
import com.saywell.backend.dto.QuotationUpdateRequest;
import com.saywell.backend.entity.Quotation;
import com.saywell.backend.service.QuotationService;
import jakarta.validation.Valid;

/**
 * 見積Controller
 */
@RestController
@RequestMapping("/api/quotations")
public class QuotationController {

    private final QuotationService quotationService;

    public QuotationController(QuotationService quotationService) {
        this.quotationService = quotationService;
    }

    /**
     * 見積詳細を取得（全ユーザーが参照可能） GET /api/quotations/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuotationDto>> findById(@PathVariable Long id) {
        try {
            QuotationDto dto = quotationService.findById(id);
            return ResponseEntity.ok(ApiResponse.success(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("見積の取得に失敗しました: " + e.getMessage()));
        }
    }

    /**
     * 作成者IDで見積一覧を取得（本人が作成した見積のみ） GET /api/quotations?createdByUserId={userId}
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Quotation>>> findByCreatedByUserId(
            @RequestParam(required = false) Integer createdByUserId) {
        try {
            List<Quotation> quotations;
            if (createdByUserId != null) {
                quotations = quotationService.findByCreatedByUserId(createdByUserId);
            } else {
                // createdByUserIdが指定されていない場合は空リストを返す
                quotations = List.of();
            }
            return ResponseEntity.ok(ApiResponse.success(quotations));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("見積一覧の取得に失敗しました: " + e.getMessage()));
        }
    }

    /**
     * 見積検索（得意先名、案件名、見積Noの部分一致、全ユーザーが参照可能） GET
     * /api/quotations/search?customerName=...&projectName=...&estimateNo=...
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Quotation>>> search(
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String projectName,
            @RequestParam(required = false) String estimateNo) {
        try {
            List<Quotation> quotations =
                    quotationService.search(customerName, projectName, estimateNo);
            return ResponseEntity.ok(ApiResponse.success(quotations));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("見積の検索に失敗しました: " + e.getMessage()));
        }
    }

    /**
     * 見積を新規保存 POST /api/quotations
     */
    @PostMapping
    public ResponseEntity<ApiResponse<QuotationDto>> create(
            @Valid @RequestBody QuotationCreateRequest request) {
        try {
            QuotationDto dto = quotationService.create(request.getQuotation(), request.getItems());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("見積を作成しました", dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("見積の作成に失敗しました: " + e.getMessage()));
        }
    }

    /**
     * 見積を更新保存（作成者本人のみ） PUT /api/quotations/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<QuotationDto>> update(@PathVariable Long id,
            @Valid @RequestBody QuotationUpdateRequest request,
            @RequestParam Integer currentUserId) {
        try {
            // リクエストの見積IDとパス変数のIDが一致することを確認
            if (request.getQuotation() != null && request.getQuotation().getId() != null
                    && !request.getQuotation().getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("見積IDが一致しません"));
            }
            // パス変数のIDを設定
            if (request.getQuotation() != null) {
                request.getQuotation().setId(id);
            }

            QuotationDto dto = quotationService.update(request.getQuotation(), request.getItems(),
                    currentUserId);
            return ResponseEntity.ok(ApiResponse.success("見積を更新しました", dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("見積の更新に失敗しました: " + e.getMessage()));
        }
    }

    /**
     * 見積を削除（作成者本人のみ） DELETE /api/quotations/{id}?currentUserId={userId}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
            @RequestParam Integer currentUserId) {
        try {
            quotationService.delete(id, currentUserId);
            return ResponseEntity.ok(ApiResponse.success("見積を削除しました", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("見積の削除に失敗しました: " + e.getMessage()));
        }
    }

    /**
     * 見積をコピーして新規作成（参照した見積から新規の自分の見積を作成） POST /api/quotations/{id}/copy
     */
    @PostMapping("/{id}/copy")
    public ResponseEntity<ApiResponse<QuotationDto>> copy(@PathVariable Long id,
            @Valid @RequestBody QuotationCopyRequest request) {
        try {
            QuotationDto dto = quotationService.copy(id, request.getNewCreatedByUserId(),
                    request.getNewUserDepartmentName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("見積をコピーして新規作成しました", dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("見積のコピーに失敗しました: " + e.getMessage()));
        }
    }
}

