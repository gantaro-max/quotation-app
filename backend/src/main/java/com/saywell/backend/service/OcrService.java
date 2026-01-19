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

        // プロンプト: 余計な行を出さないように指示しつつ、コード側でもフィルタリングする
        String promptText = """
                この見積書(PDF)の【明細行のみ】を抽出してください。

                【重要ルール】
                1. 出力は CSV形式（品名, 数量, 単価）の3列のみ。
                2. 「単価」欄について：
                   - 見積書に「単価」の記載がある場合は、その値を記入してください。
                   - 見積書に「単価」がなく「金額」のみ記載がある場合（一式など）は、「金額」の値を「単価」欄に記入してください。
                3. 明細行は上から順にすべて出力してください。

                【除外対象】
                - 「小計」「消費税」「合計」「値引き」などの集計行は出力しないでください。
                - ヘッダー行は出力しないでください。

                【出力形式】
                品名, 数量, 単価
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

            text = text.replaceAll("(?i)``` *[a-z]*", "").replaceAll("```", "").trim();
            logger.info("Gemini Extracted Text (Cleaned):\n{}", text);

            try (BufferedReader reader = new BufferedReader(new StringReader(text))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank())
                        continue;

                    String[] columns;
                    if (line.contains("|")) {
                        String cleanLine = line.replaceAll("^\\|", "").replaceAll("\\|$", "");
                        columns = cleanLine.split("\\|", -1);
                    } else if (line.contains("\t")) {
                        columns = line.split("\t", -1);
                    } else {
                        columns = line.split(",", -1);
                    }

                    if (columns.length < 1)
                        continue;

                    for (int i = 0; i < columns.length; i++)
                        columns[i] = columns[i].trim();

                    String name = columns[0];

                    // ★修正: 不要行の強力なフィルタリング
                    if (shouldSkipLine(name)) {
                        continue;
                    }

                    QuotationItem item = new QuotationItem();
                    item.setItemName(name);

                    // 数量 (2列目)
                    if (columns.length > 1)
                        item.setQuantity(parseDecimal(columns[1]));

                    // 単価 (3列目) -> 仕切価(CostPrice)へ
                    BigDecimal price = (columns.length > 2) ? parseDecimal(columns[2]) : null;

                    if (price != null) {
                        // ★修正: 仕切単価(CostPrice)のみにセットする（単価(UnitPrice)には入れない）
                        item.setCostPrice(price);
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

    // 行スキップ判定メソッド
    private boolean shouldSkipLine(String name) {
        if (name.isEmpty() || name.startsWith("-") || name.startsWith("=") || name.equals("品名")
                || name.equalsIgnoreCase("Item")) {
            return true;
        }
        // 集計行と思われるキーワードが含まれていたらスキップ
        String[] skipKeywords = {"小計", "合計", "消費税", "値引", "諸経費", "以下余白", "内訳"};
        for (String keyword : skipKeywords) {
            if (name.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank())
            return null;
        try {
            String cleaned = value.replaceAll("[,¥￥\\s円]", "");
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
