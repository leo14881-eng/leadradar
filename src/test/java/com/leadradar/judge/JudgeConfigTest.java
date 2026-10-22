package com.leadradar.judge;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JudgeConfigTest {
    private final JudgeConfig config = new JudgeConfig();

    private LeadJudge build(String provider, String key) {
        return config.leadJudge(new LlmProperties(provider, key, null, null, null), RestClient.builder(),
                new ObjectMapper());
    }

    @Test
    void noneProviderUsesKeywordOnly() {
        assertInstanceOf(KeywordOnlyJudge.class, build("none", null));
        assertInstanceOf(KeywordOnlyJudge.class, build(null, null));
    }

    @Test
    void llmProvidersNeedKey() {
        assertThrows(IllegalStateException.class, () -> build("claude", ""));
        assertInstanceOf(LlmLeadJudge.class, build("claude", "k"));
        assertInstanceOf(LlmLeadJudge.class, build("OpenAI", "k"));
    }

    @Test
    void unknownProviderFailsFast() {
        assertThrows(IllegalStateException.class, () -> build("gemini", "k"));
    }
}
