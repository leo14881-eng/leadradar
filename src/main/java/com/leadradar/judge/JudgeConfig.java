package com.leadradar.judge;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class JudgeConfig {

    @Bean
    public LeadJudge leadJudge(LlmProperties props, RestClient.Builder builder, ObjectMapper mapper) {
        return switch (props.provider()) {
            case "none" -> new KeywordOnlyJudge();
            case "claude" -> new LlmLeadJudge(new ClaudeClient(requireKey(builder, props), props), mapper);
            case "openai" -> new LlmLeadJudge(new OpenAiClient(requireKey(builder, props), props), mapper);
            default -> throw new IllegalStateException("未知的 leadradar.llm.provider: " + props.provider()
                    + "（可选 none / claude / openai）");
        };
    }

    private static RestClient.Builder requireKey(RestClient.Builder builder, LlmProperties props) {
        if (props.apiKey() == null || props.apiKey().isBlank()) {
            throw new IllegalStateException("leadradar.llm.provider=" + props.provider()
                    + " 需要设置 LLM_API_KEY 环境变量");
        }
        return builder.clone();
    }
}
