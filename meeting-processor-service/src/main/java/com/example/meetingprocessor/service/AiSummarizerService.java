package com.example.meetingprocessor.service;

import com.example.meetingprocessor.dto.AiSummaryResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class AiSummarizerService {

    private static final Logger log = LoggerFactory.getLogger(AiSummarizerService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestClient restClient;
    private final String model;
    private final String apiKey;

    public AiSummarizerService(
            @Value("${openai.api.key}") String apiKey,
            @Value("${openai.api.base-url}") String baseUrl,
            @Value("${openai.api.model}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    AiSummarizerService(String apiKey, String model, RestClient restClient) {
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = restClient;
    }

    String callOpenAi(Map<String, Object> body) {
        return restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
    }

    public AiSummaryResult summarize(String transcript) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OPENAI_API_KEY not configured — returning placeholder summary");
            return new AiSummaryResult(
                    "AI summarization not configured",
                    "Set the OPENAI_API_KEY environment variable to enable AI summaries.",
                    List.of(), List.of(), List.of()
            );
        }

        String systemPrompt = """
                You are an expert meeting summarizer. Analyze the transcript and respond ONLY with valid JSON \
                using exactly these fields (no markdown, no explanation):
                {
                  "shortSummary": "1-2 sentence overview",
                  "detailedSummary": "comprehensive paragraph summary",
                  "actionItems": ["action item 1", "action item 2"],
                  "decisions": ["decision 1"],
                  "blockers": ["blocker 1"]
                }
                Use empty arrays when there are no items for a list field.""";

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", "Meeting transcript:\n\n" + transcript)
                ),
                "response_format", Map.of("type", "json_object")
        );

        try {
            String raw = callOpenAi(body);

            JsonNode root = MAPPER.readTree(raw);
            String content = root.path("choices").get(0).path("message").path("content").asText();
            return MAPPER.readValue(content, AiSummaryResult.class);
        } catch (Exception e) {
            log.error("AI summarization failed", e);
            return new AiSummaryResult(
                    "Summarization failed",
                    "An error occurred: " + e.getMessage(),
                    List.of(), List.of(), List.of()
            );
        }
    }
}
