package com.leadradar.judge;

import com.leadradar.lead.Lead;
import com.leadradar.lead.Post;
import com.leadradar.merchant.Merchant;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 占位实现：不调模型，规则命中即推送原文。接入 Claude API 后新增 ClaudeLeadJudge 并删除本类（或改为 @Primary）。
 */
@Component
public class PassThroughJudge implements LeadJudge {
    @Override
    public Optional<Lead> judge(Post post, Merchant merchant) {
        var text = post.text();
        var summary = text.length() > 200 ? text.substring(0, 200) + "…" : text;
        return Optional.of(new Lead(post, merchant, summary, "（待接入模型生成建议回复）", 50));
    }
}
