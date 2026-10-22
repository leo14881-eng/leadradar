package com.leadradar.judge;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/** Anthropic Messages API。 */
public class ClaudeClient implements LlmClient {
    static final String DEFAULT_MODEL = "claude-haiku-5-5";
    private final RestClient http;
    private final String model;

    public ClaudeClient(RestClient.Builder builder, LlmProperties props) {
        this.model = blankTo(props.model(), DEFAULT_MODEL);
        this.http = builder
                .baseUrl(blankTo(props.baseUrl(), "https://api.anthropic.com"))
                .defaultHeader("x-api-key", props.apiKey())
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
    }

    @Override
    public String complete(String system, String user) {
        var body = Map.of(
                "model", model,
                "max_tokens", 800,
                "system", system,
                "messages", List.of(Map.of("role", "user", "content", user)));
        JsonNode res = http.post().uri("/v1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        var sb = new StringBuilder();
        if (res != null) {
            for (JsonNode block : res.path("content")) {
                if ("text".equals(block.path("type").asText())) {
                    sb.append(block.path("text").asText());
                }
            }
        }
        return sb.toString();
    }

    static String blankTo(String v, String dflt) {
        return v == null || v.isBlank() ? dflt : v;
    }
}
