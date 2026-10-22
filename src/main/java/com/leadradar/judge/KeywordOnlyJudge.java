package com.leadradar.judge;

import com.leadradar.lead.Lead;
import com.leadradar.lead.Post;
import com.leadradar.merchant.Merchant;

import java.util.Optional;

/** provider=none 时使用：不调模型，关键词命中即推送原文。 */
public class KeywordOnlyJudge implements LeadJudge {
    @Override
    public Optional<Lead> judge(Post post, Merchant merchant) {
        return Optional.of(rawLead(post, merchant, "（未启用模型，无建议回复）"));
    }

    static Lead rawLead(Post post, Merchant merchant, String replyNote) {
        var text = post.text();
        var summary = text.length() > 200 ? text.substring(0, 200) + "…" : text;
        return Lead.unscored(post, merchant, summary, replyNote);
    }
}
