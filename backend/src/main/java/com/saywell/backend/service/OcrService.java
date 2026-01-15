package com.saywell.backend.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(OcrService.class);

    @Value("${gemini.api.key}")
    private String apiKey;

    private static final String GEMINI_API_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=%s";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public OcrService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    public OcrService(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public List<QuotationItem> analyzeFile(MultipartFile file)
            throws IOException, InterruptedException {

        String base64Data = Base64.getEncoder().encodeToString(file.getBytes());
        String mimeType = file.getContentType();
        if (mimeType == null || "application/octet-stream".equals(mimeType)) {
            mimeType = "application/pdf";
        }

        logger.info("OCR Request: {} ({})", file.getOriginalFilename(), mimeType);

        String promptText = """
                この見積書のPDFから、明細行（品名、数量、単価[単価がなければ金額]）を抽出してください。

                【出力形式】
                CSVフォーマット（ヘッダー: 品名, 数量, 単価）

                【制約】
                - ヘッダー行は不要です。データのみ返してください。
                - 余計な文章やMarkdownタグは含めないでください。
                - 数値には円マークやカンマを含めないでください。
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

        if (response.statusCode() != 200) {
            logger.error("Gemini API Error: {} {}", response.statusCode(), response.body());
            throw new RuntimeException("Gemini API Error: " + response.statusCode());
        }

        return parseGeminiResponseText(response.body());
    }

    private List<QuotationItem> parseGeminiResponseText(String responseBody) {
        List<QuotationItem> items = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String text = root.path("candidates").get(0).path("content").path("parts").get(0)
                    .path("text").asText();

            // 1. Markdownコードブロックの除去 (```csv ... ``` や ``` ... ```)
            // 大文字小文字無視、スペース許容で強力に削除
            text = text.replaceAll("(?i)``` *[a-z]*", "").replaceAll("```", "").trim();

            logger.info("Gemini Extracted Text (Cleaned):\n{}", text);

            try (BufferedReader reader = new BufferedReader(new StringReader(text))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank())
                        continue;

                    // 2. 区切り文字の柔軟な判定
                    String[] columns;
                    if (line.contains("|")) {
                        // Markdownテーブル形式 (| 品名 | 数量 |...) の場合
                        // 先頭と末尾の | を削除してから分割
                        String cleanLine = line.replaceAll("^\\|", "").replaceAll("\\|$", "");
                        columns = cleanLine.split("\\|", -1);
                    } else if (line.contains("\t")) {
                        // タブ区切りの場合
                        columns = line.split("\t", -1);
                    } else {
                        // デフォルト: カンマ区切り
                        // (注: 品名の中にカンマがある場合などは簡易分割のため崩れる可能性があります)
                        columns = line.split(",", -1);
                    }

                    if (columns.length < 1)
                        continue;

                    // 各カラムの空白除去
                    for (int i = 0; i < columns.length; i++) {
                        columns[i] = columns[i].trim();
                    }

                    String name = columns[0];

                    // ヘッダー行や区切り行（---）のスキップ
                    if (name.isEmpty() || name.startsWith("-") || name.equals("品名")
                            || name.equalsIgnoreCase("Item")) {
                        continue;
                    }

                    QuotationItem item = new QuotationItem();
                    item.setItemName(name);

                    // 数量 (2列目)
                    if (columns.length > 1)
                        item.setQuantity(parseDecimal(columns[1]));

                    // 単価・金額 (3列目、4列目)
                    BigDecimal unitPrice = (columns.length > 2) ? parseDecimal(columns[2]) : null;
                    BigDecimal amount = (columns.length > 3) ? parseDecimal(columns[3]) : null;

                    // 仕入見積のロジック: 金額があればそれを仕切単価(CostPrice)に、なければ単価を採用
                    if (amount != null) {
                        item.setCostPrice(amount);
                    } else if (unitPrice != null) {
                        item.setCostPrice(unitPrice);
                    }

                    item.setRowType("normal");
                    items.add(item);
                }
            }
            logger.info("Parsed {} items.", items.size());

        } catch (Exception e) {
            logger.error("Parse Error: {}", e.getMessage(), e);
        }
        return items;
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank())
            return null;
        try {
            // 円マーク、カンマ、スペース、"円"などの文字を除去して数値化
            String cleaned = value.replaceAll("[,¥￥\\s円]", "");
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null; // 数値変換できない場合はnullを返す
        }
    }
}
