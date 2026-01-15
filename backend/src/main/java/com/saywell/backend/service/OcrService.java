package com.saywell.backend.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saywell.backend.dto.gemini.GeminiContent;
import com.saywell.backend.dto.gemini.GeminiInlineData;
import com.saywell.backend.dto.gemini.GeminiPart;
import com.saywell.backend.dto.gemini.GeminiRequest;
import com.saywell.backend.entity.QuotationItem;

@Service
public class OcrService {
    @Value("${gemini.api.key}")
    private String apiKey;

    // 単独アプリで成功したモデル名を使用
    private static final String GEMINI_API_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public OcrService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    // テスト用コンストラクタ
    public OcrService(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public List<QuotationItem> analyzeFile(MultipartFile file)
            throws IOException, InterruptedException {

        String base64Data = Base64.getEncoder().encodeToString(file.getBytes());

        // MIMEタイプが取得できない、またはoctet-streamの場合はPDFとみなす
        String mimeType = file.getContentType();
        if (mimeType == null || "application/octet-stream".equals(mimeType)) {
            mimeType = "application/pdf";
        }

        System.out.println("OCR Request: " + file.getOriginalFilename() + " (" + mimeType + ")");

        // プロンプト：JSON配列のみを強く要求
        String promptText = """
                この見積書(PDF/画像)から、明細行[品名、数量、単価(単価がない場合、金額)]を抽出してください。

                【出力ルール】
                1. 結果は必ず JSONの配列形式 `[...]` のみにしてください。
                2. 余計な挨拶やMarkdownタグ（```json 等）は極力含めないでください。
                3. 各オブジェクトのキーは "itemName", "quantity", "unitPrice" としてください。
                4. 数量と単価は数値型にしてください（円マークやカンマは除去）。

                例:
                [{"itemName":"商品A", "quantity":1, "unitPrice":1000}]
                """;

        GeminiRequest requestPayload = new GeminiRequest(List.of(new GeminiContent(
                List.of(new GeminiPart(null, new GeminiInlineData(mimeType, base64Data)),
                        new GeminiPart(promptText, null)))));

        String jsonBody = objectMapper.writeValueAsString(requestPayload);
        String apiUrl = String.format(GEMINI_API_URL_TEMPLATE, apiKey);

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // デバッグ用ログ出力
        if (response.statusCode() != 200) {
            System.err.println("Gemini API Error Status: " + response.statusCode());
            System.err.println("Gemini API Error Body: " + response.body());
            throw new RuntimeException(
                    "Gemini API Error: " + response.statusCode() + " " + response.body());
        }

        // 成功時もレスポンスをログに出して確認できるようにする
        // System.out.println("Gemini Response: " + response.body());

        return parseGeminiResponse(response.body());
    }

    private List<QuotationItem> parseGeminiResponse(String responseBody) {
        List<QuotationItem> items = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            // テキスト部分を取得
            String text = root.path("candidates").get(0).path("content").path("parts").get(0)
                    .path("text").asText();

            // ★修正: 正規表現ではなく、最初の '[' から 最後の ']' までを切り出す（最も確実）
            int start = text.indexOf("[");
            int end = text.lastIndexOf("]");

            if (start == -1 || end == -1) {
                System.err.println("JSON配列が見つかりませんでした: " + text);
                return items; // 空リストを返す
            }

            String jsonArrayStr = text.substring(start, end + 1);

            JsonNode arrayNode = objectMapper.readTree(jsonArrayStr);
            if (arrayNode.isArray()) {
                for (JsonNode node : arrayNode) {
                    QuotationItem item = new QuotationItem();
                    item.setItemName(node.path("itemName").asText(""));
                    // 数値変換時にnull安全にする
                    if (node.has("quantity") && !node.get("quantity").isNull()) {
                        item.setQuantity(node.path("quantity").decimalValue());
                    }
                    if (node.has("unitPrice") && !node.get("unitPrice").isNull()) {
                        item.setUnitPrice(node.path("unitPrice").decimalValue());
                    }
                    item.setRowType("normal");
                    items.add(item);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // パースエラー時も空リストなどを返してアプリを落とさないようにする
            System.err.println("Parse Error: " + e.getMessage());
        }
        return items;
    }
}
