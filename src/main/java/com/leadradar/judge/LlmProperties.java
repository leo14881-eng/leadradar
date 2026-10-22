package com.leadradar.judge;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param provider none | claude | openai。none 时不调模型，关键词命中即推送原文。
 * @param apiKey   对应厂商的 API key，建议用环境变量 LLM_API_KEY 注入
 * @param model    模型名，留空用各厂商默认值
 * @param baseUrl  留空用官方地址；可指向兼容 OpenAI 协议的其他服务
 * @param minScore 低于此分数的线索不推送
 */
@ConfigurationProperties(prefix = "leadradar.llm")
public record LlmProperties(String provider, String apiKey, String model, String baseUrl, Integer minScore) {
    public LlmProperties {
        provider = provider == null || provider.isBlank() ? "none" : provider.trim().toLowerCase();
        minScore = minScore == null ? 60 : minScore;
    }
}
