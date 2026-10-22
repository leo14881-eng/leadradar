package com.leadradar.judge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadradar.lead.Post;
import com.leadradar.merchant.Merchant;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmLeadJudgeTest {
    private final Merchant merchant = new Merchant("m1", "川味馆", "中餐厅", List.of("Quận 7"),
            List.of("món trung"), List.of(), null, "正宗川菜，第7郡免费外送");
    private final Post post = new Post("fb", "1", "https://x/1", "a",
            "Tối nay 4 người muốn ăn lẩu Tứ Xuyên ở Quận 7, quán nào ngon?", Instant.now());

    private LlmLeadJudge judgeReturning(String output) {
        return new LlmLeadJudge((system, user) -> output, new ObjectMapper());
    }

    @Test
    void parsesRelevantLeadInsideCodeFence() {
        var out = """
                好的，结果如下：
                ```json
                {"relevant": true, "score": 88, "summary_zh": "今晚4人想在第7郡吃川味火锅",
                 "reply_vi": "Dạ quán em có lẩu Tứ Xuyên ở Quận 7 ạ, anh/chị ghé nhé!"}
                ```""";
        var lead = judgeReturning(out).judge(post, merchant).orElseThrow();
        assertEquals(88, lead.score());
        assertEquals("今晚4人想在第7郡吃川味火锅", lead.summaryZh());
        assertTrue(lead.suggestedReply().startsWith("Dạ quán em"));
    }

    @Test
    void irrelevantPostIsDropped() {
        var out = "{\"relevant\": false, \"score\": 10, \"summary_zh\": \"同行广告\", \"reply_vi\": \"\"}";
        assertTrue(judgeReturning(out).judge(post, merchant).isEmpty());
    }

    @Test
    void scoreIsClampedTo0To100() {
        var out = "{\"relevant\": true, \"score\": 150, \"summary_zh\": \"x\", \"reply_vi\": \"y\"}";
        assertEquals(100, judgeReturning(out).judge(post, merchant).orElseThrow().score());
    }

    @Test
    void unparseableOutputFallsBackToRawPost() {
        var lead = judgeReturning("抱歉我无法判断").judge(post, merchant).orElseThrow();
        assertEquals(post.text(), lead.summaryZh());
        assertFalse(lead.modelScored());
    }

    @Test
    void clientFailureFallsBackToRawPost() {
        LlmClient failing = (s, u) -> {
            throw new IllegalStateException("timeout");
        };
        var lead = new LlmLeadJudge(failing, new ObjectMapper()).judge(post, merchant).orElseThrow();
        assertTrue(lead.suggestedReply().contains("模型调用失败"));
    }

    @Test
    void promptContainsMerchantPitchAndPostText() {
        var captured = new AtomicReference<String>();
        new LlmLeadJudge((s, u) -> {
            captured.set(u);
            return "{}";
        }, new ObjectMapper()).judge(post, merchant);
        assertTrue(captured.get().contains("正宗川菜，第7郡免费外送"));
        assertTrue(captured.get().contains("lẩu Tứ Xuyên"));
    }
}
