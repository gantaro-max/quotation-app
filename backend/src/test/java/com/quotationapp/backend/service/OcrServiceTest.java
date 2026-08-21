package com.quotationapp.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    ocrService = new OcrService(objectMapper, httpClient);
  }

  @Test
  @DisplayName("analyzeFile: 正常なCSVレスポンスを解析できること")
  void testAnalyzeFile_Success() throws IOException, InterruptedException {
    // 1. 準備: Gemini APIが返すCSV形式のモックデータ
    String mockCsvResponse = """
        {
          "candidates": [
            {
              "content": {
                "parts": [
                  {
                    "text": "```csv\\nテスト商品A, 10, 500\\n小計, , 5000\\n合計, , 5500\\n```"
                  }
                ]
              }
            }
          ]
        }
        """;

    // Mockの設定
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(mockCsvResponse);
    when(httpClient.send(any(HttpRequest.class),
        ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(httpResponse);

    MockMultipartFile file =
        new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy content".getBytes());

    // 2. 実行
    List<QuotationItem> result = ocrService.analyzeFile(file);

    // 3. 検証
    assertNotNull(result);
    assertEquals(1, result.size()); // 小計・合計は除外されるので1件になるはず

    QuotationItem item1 = result.get(0);
    assertEquals("テスト商品A", item1.getItemName());
    assertEquals(0, BigDecimal.valueOf(10).compareTo(item1.getQuantity()));
    assertEquals(0, BigDecimal.valueOf(500).compareTo(item1.getCostPrice()));

    // ★修正: UnitPriceにはセットされないこと（null または 0 であること）を確認
    // 初期値がnullの場合はassertNull、BigDecimal.ZEROの場合は0比較
    assertNull(item1.getUnitPrice());
  }

  @Test
  @DisplayName("analyzeFile: APIエラー時は例外を投げること")
  void testAnalyzeFile_ApiError() throws IOException, InterruptedException {
    when(httpResponse.statusCode()).thenReturn(500);
    when(httpResponse.body()).thenReturn("Internal Server Error");
    when(httpClient.send(any(HttpRequest.class),
        ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(httpResponse);

    MockMultipartFile file =
        new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy".getBytes());

    assertThrows(RuntimeException.class, () -> ocrService.analyzeFile(file));
  }
}
