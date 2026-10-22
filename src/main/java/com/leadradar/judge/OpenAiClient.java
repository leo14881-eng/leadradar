package com.leadradar.judge;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/** OpenAI Chat Completions API（也适用于兼容该协议的服务）。 */
public class OpenAiClient implements LlmClient {
    static final String DEFAULT_MODEL = "gpt-5-mini";
    private final RestClient http;
    private final String model;

    public OpenAiClient(RestClient.Builder builder, LlmProperties props) {
        this.model = ClaudeClient.blankTo(props.model(), DEFAULT_MODEL);
        this.http = builder
                .baseUrl(ClaudeClient.blankTo(props.baseUrl(), "https://api.openai.com"))
                .defaultHeader("Authorization", "Bearer " + props.apiKey())
                .build();
    }

    @Override
    public String complete(String system, String user) {
        var body = Map.of(
                "model", model,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", user)));
        JsonNode res = http.post().uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        return res == null ? "" : res.path("choices").path(0).path("message").path("content").asText("");
    }
}
