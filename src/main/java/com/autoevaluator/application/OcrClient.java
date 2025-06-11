package com.autoevaluator.application;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.multipart.MultipartFile;

@Component
public class OcrClient {

    private final WebClient webClient;

    public OcrClient() {
        // Load from environment or fallback to localhost
        String ocrUrl = System.getenv("OCR_URL");
        if (ocrUrl == null || ocrUrl.isBlank()) {
            ocrUrl = Dotenv.configure().ignoreIfMissing().load().get("OCR_URL", "http://localhost:8000");
        }
        this.webClient = WebClient.create(ocrUrl);
    }

    public String extractText(MultipartFile file) {
        try {
            byte[] fileBytes = file.getBytes();
            ByteArrayResource resource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            var response = webClient.post()
                    .uri("/getTextFromImage/")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData("file", resource))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .map(json -> json.get("extracted_text").asText())
                    .block();

            System.out.println(response);
            return response;

        } catch (Exception e) {
            throw new RuntimeException("Failed to call OCR API: " + e.getMessage(), e);
        }
    }
}
