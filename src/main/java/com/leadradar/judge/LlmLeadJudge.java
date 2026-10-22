package com.leadradar.judge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadradar.lead.Lead;
import com.leadradar.lead.Post;
import com.leadradar.merchant.Merchant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * 用大模型判断帖子是否为商家的潜在客户，生成中文摘要和越南语建议回复。
 * 模型调用失败或输出无法解析时，退化为"关键词命中即推送原文"，避免漏掉线索。
 */
public class LlmLeadJudge implements LeadJudge {
    private static final Logger log = LoggerFactory.getLogger(LlmLeadJudge.class);

    static final String SYSTEM_PROMPT = """
            你是越南胡志明市本地生意的线索分析助手，帮中国老板判断社交媒体帖子是不是他的潜在客户。
            判断标准：发帖人是在【寻找/求购/求租/询问】该商家能提供的商品或服务，才算潜在客户。
            以下情况都不是潜在客户：同行打广告、招聘、转让店铺、新闻、闲聊、发帖人自己在卖东西。
            只输出一个 JSON 对象，不要输出其他文字，字段如下：
            {
              "relevant": true 或 false,
              "score": 0 到 100 的整数，表示是潜在客户的把握,
              "summary_zh": "用一句中文概括发帖人要什么、在哪、什么时候、几个人或预算（帖子里没有的不要编）",
              "reply_vi": "以商家身份写给发帖人的越南语回复，口语自然、礼貌，像本地商家，60 字以内；结合商家卖点，不要编造帖子和卖点里没有的价格或承诺"
            }
            relevant 为 false 时 reply_vi 留空字符串。
            """;

    private final LlmClient client;
    private final ObjectMapper mapper;

    public LlmLeadJudge(LlmClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public Optional<Lead> judge(Post post, Merchant merchant) {
        String raw;
        try {
            raw = client.complete(SYSTEM_PROMPT, userPrompt(post, merchant));
        } catch (RuntimeException e) {
            log.warn("LLM call failed for {}, falling back to raw post: {}", post.dedupKey(), e.toString());
            return Optional.of(KeywordOnlyJudge.rawLead(post, merchant, "（模型调用失败，未生成回复）"));
        }
        JsonNode json = parse(raw);
        if (json == null) {
            log.warn("Unparseable LLM output for {}: {}", post.dedupKey(), raw);
            return Optional.of(KeywordOnlyJudge.rawLead(post, merchant, "（模型输出无法解析，未生成回复）"));
        }
        if (!json.path("relevant").asBoolean(false)) {
            return Optional.empty();
        }
        int score = Math.max(0, Math.min(100, json.path("score").asInt(0)));
        return Optional.of(new Lead(post, merchant,
                json.path("summary_zh").asText(""),
                json.path("reply_vi").asText(""),
                score));
    }

    static String userPrompt(Post post, Merchant merchant) {
        return """
                【商家】%s（%s）
                服务区域：%s
                卖点：%s

                【帖子】来源：%s
                %s
                """.formatted(merchant.name(), merchant.category(), String.join("、", merchant.districts()),
                merchant.pitch() == null ? "" : merchant.pitch(), post.source(), post.text());
    }

    /** 容忍模型在 JSON 前后加说明文字或 ```json 代码块。 */
    JsonNode parse(String raw) {
        if (raw == null) {
            return null;
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        try {
            return mapper.readTree(raw.substring(start, end + 1));
        } catch (Exception e) {
            return null;
        }
    }
}
