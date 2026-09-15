package com.quotationapp.backend.dto.gemini;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeminiInlineData {
    @JsonProperty("mime_type")
    private String mimeType;
    private String data;

}
