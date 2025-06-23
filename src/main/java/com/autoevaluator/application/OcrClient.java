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
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Component
public class OcrClient {

    private final WebClient webClient;

    public OcrClient() {
        // Load from environment or fallback
        String ocrUrl = System.getenv("OCR_URL");
        if (ocrUrl == null || ocrUrl.isBlank()) {
            ocrUrl = Dotenv.configure().ignoreIfMissing().load().get("OCR_URL", "http://localhost:8000");
        }

        // Configure WebClient with 20s timeout
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(20));

        this.webClient = WebClient.builder()
                .baseUrl(ocrUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    public String extractText(List<MultipartFile> files) {
        try {
            MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();

            for (MultipartFile file : files) {
                byte[] fileBytes = file.getBytes();
                ByteArrayResource resource = new ByteArrayResource(fileBytes) {
                    @Override
                    public String getFilename() {
                        return file.getOriginalFilename(); // Required for multipart
                    }
                };
                formData.add("files", resource); // Match FastAPI param name: "files"
            }

            JsonNode json = webClient.post()
                    .uri("/getTextFromImage/")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(formData))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(20)) // Per-request timeout
                    .block();

            if (json == null) {
                throw new RuntimeException("OCR API returned no response");
            }

            if (!json.has("extracted_text")) {
                throw new RuntimeException("OCR API response missing 'extracted_text' field: " + json);
            }

            return json.get("extracted_text").asText();

        } catch (Exception e) {
            if (isTimeoutException(e)) {
                throw new RuntimeException("OCR request timed out after 20 seconds", e);
            }

            throw new RuntimeException("Failed to call OCR API: " + e.getMessage(), e);
        }
    }

    // ✅ Helper to check if the root cause is a timeout
    private boolean isTimeoutException(Throwable e) {
        while (e != null) {
            if (e instanceof TimeoutException) return true;
            e = e.getCause();
        }
        return false;
    }
}
