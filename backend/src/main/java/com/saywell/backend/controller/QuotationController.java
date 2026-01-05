package com.saywell.backend.controller;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saywell.backend.dto.ApiResponse;
import com.saywell.backend.dto.QuotationCopyRequest;
import com.saywell.backend.dto.QuotationDto;
import com.saywell.backend.service.QuotationService;
import lombok.RequiredArgsConstructor;

/**
 * 見積Controller
 */
@RestController
@RequestMapping("/api/quotations")
@RequiredArgsConstructor
public class QuotationController {

    private final QuotationService quotationService;

    private final ObjectMapper objectMapper; // JSON変換用

    // 保存先ディレクトリ (プロジェクト直下の uploads フォルダ)
    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/";

    // --- ファイル保存用のヘルパーメソッド ---
    private String saveFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty())
            return null;

        // フォルダがない場合は作成
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists())
            dir.mkdirs();

        // ファイル名が重複しないように現在時刻を付与
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        File dest = new File(UPLOAD_DIR + fileName);

        // 保存実行
        file.transferTo(dest);

        return fileName; // DBに保存するパス（ファイル名）
    }

    // --- 新規作成 (POST) ---
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ApiResponse<QuotationDto>> create(
            @RequestPart("quotation") String quotationJson, // JSON文字列として受け取る
            @RequestPart(value = "file", required = false) MultipartFile file) {
        try {
            // 文字列JSONをDTOに変換
            QuotationDto dto = objectMapper.readValue(quotationJson, QuotationDto.class);

            // ファイルがあれば保存してパスをセット
            if (file != null) {
                String filePath = saveFile(file);
                dto.setAttachedFilePath(filePath);
            }

            QuotationDto result = quotationService.create(dto);
            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(ApiResponse.error("保存失敗: " + e.getMessage()));
        }
    }

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
     * 見積検索（全ユーザーが参照可能） GET /api/quotations/search 引数に customerCode, salesBranchName を追加
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<QuotationDto>>> search(
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String customerCode,
            @RequestParam(required = false) String salesBranchName,
            @RequestParam(required = false) String projectName,
            @RequestParam(required = false) String estimateNo) {

        System.out.println("Search Params: code=" + customerCode + ", name=" + customerName);

        List<QuotationDto> list = quotationService.search(customerName, customerCode,
                salesBranchName, projectName, estimateNo);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    // =========================================================================
    // 更新系
    // =========================================================================
    /**
     * 見積を更新保存（作成者本人のみ） PUT /api/quotations/{id}
     */
    @PutMapping(value = "/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ApiResponse<QuotationDto>> update(@PathVariable Long id,
            @RequestPart("quotation") String quotationJson,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestParam Integer currentUserId) {
        try {
            QuotationDto dto = objectMapper.readValue(quotationJson, QuotationDto.class);

            // ファイルがアップロードされた場合のみ保存処理
            if (file != null) {
                String filePath = saveFile(file);
                dto.setAttachedFilePath(filePath);
            }

            QuotationDto result = quotationService.update(id, dto, currentUserId);
            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(ApiResponse.error("更新失敗: " + e.getMessage()));
        }
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
