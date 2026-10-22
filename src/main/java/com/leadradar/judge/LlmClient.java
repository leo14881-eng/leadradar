package com.leadradar.judge;

/**
 * 最小化的大模型调用抽象：给系统提示词和用户内容，返回模型的文本输出。
 */
public interface LlmClient {
    String complete(String system, String user);
}
