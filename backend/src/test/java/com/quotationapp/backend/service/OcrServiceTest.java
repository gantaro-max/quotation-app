package com.quotationapp.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quotationapp.backend.entity.QuotationItem;

@ExtendWith(MockitoExtension.class)
@DisplayName("OcrServiceテスト")
class OcrServiceTest {

    private OcrService ocrService;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // テスト用コンストラクタを使用してMockを注入
        ocrService = new OcrService(objectMapper, httpClient);
    }

    @Test
    @DisplayName("analyzeFile: 正常なJSONレスポンスを解析できること")
    void testAnalyzeFile_Success() throws IOException, InterruptedException {
        // 1. 準備: Gemini APIが返してくるであろうJSON文字列
        String mockJsonResponse =
                """
                        {
                          "candidates": [
                            {
                              "content": {
                                "parts": [
                                  {
                                    "text": "```json\\n[ {\\"itemName\\": \\"テスト商品A\\", \\"quantity\\": 10, \\"unitPrice\\": 500}, {\\"itemName\\": \\"テスト商品B\\", \\"quantity\\": 1, \\"unitPrice\\": 10000} ]\\n```"
                                  }
                                ]
                              }
                            }
                          ]
                        }
                        """;

        // Mockの設定
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(mockJsonResponse);
        when(httpClient.send(any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(httpResponse);

        // テスト用ファイル
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf",
                "dummy content".getBytes());

        // 2. 実行
        List<QuotationItem> result = ocrService.analyzeFile(file);

        // 3. 検証
        assertNotNull(result);
        assertEquals(2, result.size());

        QuotationItem item1 = result.get(0);
        assertEquals("テスト商品A", item1.getItemName());
        assertEquals(BigDecimal.valueOf(10), item1.getQuantity());
        assertEquals(BigDecimal.valueOf(500), item1.getUnitPrice());
        assertEquals("normal", item1.getRowType());
    }

    @Test
    @DisplayName("analyzeFile: APIエラー時は例外を投げること")
    void testAnalyzeFile_ApiError() throws IOException, InterruptedException {
        // Mockの設定 (500エラー)
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpResponse.body()).thenReturn("Internal Server Error");
        when(httpClient.send(any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(httpResponse);

        MockMultipartFile file =
                new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy".getBytes());

        // 実行 & 検証
        assertThrows(RuntimeException.class, () -> ocrService.analyzeFile(file));
    }
}
