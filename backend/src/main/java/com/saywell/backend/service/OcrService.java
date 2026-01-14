package com.saywell.backend.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
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

    private static final String GEMINI_API_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=";

    private final ObjectMapper objectMapper;

    public OcrService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<QuotationItem> analyzeFile(MultipartFile file)
            throws IOException, InterruptedException {

        String base64Data = Base64.getEncoder().encodeToString(file.getBytes());
        String mimeType = file.getContentType();

        String promptText = """
                この見積書(PDF/画像)から、明細行[品名、数量、単価(単価がない場合、金額)]を抽出してください。
                出力形式: JSONフォーマットの配列のみ。
                各オブジェクトのキーは "itemName", "quantity", "unitPrice" としてください。
                数量と単価は数値で、円マークやカンマは除外してください。
                Markdownタグ(```json等)は含めず、純粋なJSON文字列のみを返してください。
                """;

        GeminiRequest requestPayload = new GeminiRequest(List.of(new GeminiContent(
                List.of(new GeminiPart(null, new GeminiInlineData(mimeType, base64Data)),
                        new GeminiPart(promptText, null)))));

        String jsonBody = objectMapper.writeValueAsString(requestPayload);

        String apiUrl = String.format(GEMINI_API_URL_TEMPLATE, apiKey);

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "Gemini API Error: " + response.statusCode() + " " + response.body());
        }

        return parseGeminiResponse(response.body());

    }

    private List<QuotationItem> parseGeminiResponse(String responseBody) {

        List<QuotationItem> items = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String text = root.path("candidats").get(0).path("content").path("parts").get(0)
                    .path("text").asText();
            text = text.replaceAll("^```json", "").replaceAll("```$", "").trim();

            JsonNode arrayNode = objectMapper.readTree(text);
            if (arrayNode.isArray()) {
                for (JsonNode node : arrayNode) {
                    QuotationItem item = new QuotationItem();
                    item.setItemName(node.path("itemName").asText(""));
                    item.setQuantity(node.path("quantity").decimalValue());
                    item.setUnitPrice(node.path("unitPrice").decimalValue());
                    item.setRowType("nomal");
                    items.add(item);
                }
            }



        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;

    }

}
