package com.leadradar.lead;

import com.leadradar.merchant.Merchant;

/**
 * 一条推给某个商家的线索。
 *
 * @param post           原帖
 * @param merchant       目标商家
 * @param summaryZh      中文摘要
 * @param suggestedReply 建议回复（越南语，老板确认后自己发）
 * @param score          匹配度 0-100（modelScored=false 时无意义）
 * @param modelScored    是否经过模型打分
 */
public record Lead(Post post, Merchant merchant, String summaryZh, String suggestedReply, int score,
                   boolean modelScored) {

    /** 模型打过分的线索。 */
    public Lead(Post post, Merchant merchant, String summaryZh, String suggestedReply, int score) {
        this(post, merchant, summaryZh, suggestedReply, score, true);
    }

    /** 未经模型打分（未启用模型或模型失败）的线索，不受 min-score 限制，宁可多推不漏。 */
    public static Lead unscored(Post post, Merchant merchant, String summaryZh, String note) {
        return new Lead(post, merchant, summaryZh, note, 0, false);
    }
}
