package com.autoevaluator.application;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
public class OcrClient {

    private final WebClient webClient;

    public OcrClient() {
        // Load from environment or fallback
        String ocrUrl = System.getenv("OCR_URL");
        if (ocrUrl == null || ocrUrl.isBlank()) {
            ocrUrl = Dotenv.configure().ignoreIfMissing().load().get("OCR_URL", "http://localhost:8000");
        }
        this.webClient = WebClient.builder().baseUrl(ocrUrl).build();  // ✅ FIXED HERE
    }

    // ✅ Multi-file version
    public String extractText(List<MultipartFile> files) {
        try {
            MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();

            for (MultipartFile file : files) {
                byte[] fileBytes = file.getBytes();
                ByteArrayResource resource = new ByteArrayResource(fileBytes) {
                    @Override
                    public String getFilename() {
                        return file.getOriginalFilename(); // Required
                    }
                };
                formData.add("files", resource);  // Must match FastAPI param name: "files"
            }

            JsonNode json = webClient.post()
                    .uri("/getTextFromImage/")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(formData))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (json != null && json.has("extracted_text")) {
                return json.get("extracted_text").asText();
            }

            throw new RuntimeException("OCR API did not return expected response");

        } catch (Exception e) {
            throw new RuntimeException("Failed to call OCR API: " + e.getMessage(), e);
        }
    }
}
