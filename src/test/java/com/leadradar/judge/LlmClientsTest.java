package com.leadradar.judge;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class LlmClientsTest {

    @Test
    void claudeClientSendsMessagesRequestAndJoinsTextBlocks() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(POST))
                .andExpect(header("x-api-key", "k-123"))
                .andExpect(header("anthropic-version", "2023-06-01"))
                .andExpect(jsonPath("$.model").value(ClaudeClient.DEFAULT_MODEL))
                .andExpect(jsonPath("$.system").value("SYS"))
                .andExpect(jsonPath("$.messages[0].role").value("user"))
                .andExpect(jsonPath("$.messages[0].content").value("USER"))
                .andRespond(withSuccess("""
                        {"content":[{"type":"text","text":"{\\"a\\":"},{"type":"text","text":"1}"}]}""",
                        MediaType.APPLICATION_JSON));

        var client = new ClaudeClient(builder, new LlmProperties("claude", "k-123", null, null, null));
        assertEquals("{\"a\":1}", client.complete("SYS", "USER"));
        server.verify();
    }

    @Test
    void openAiClientSendsChatCompletionAndReadsFirstChoice() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://example.test/v1/chat/completions"))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer sk-1"))
                .andExpect(jsonPath("$.model").value("my-model"))
                .andExpect(jsonPath("$.response_format.type").value("json_object"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].content").value("USER"))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"role":"assistant","content":"{\\"ok\\":true}"}}]}""",
                        MediaType.APPLICATION_JSON));

        var client = new OpenAiClient(builder,
                new LlmProperties("openai", "sk-1", "my-model", "https://example.test", null));
        assertEquals("{\"ok\":true}", client.complete("SYS", "USER"));
        server.verify();
    }
}
