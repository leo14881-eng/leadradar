package com.leadradar.judge;

import com.leadradar.lead.Lead;
import com.leadradar.lead.Post;
import com.leadradar.merchant.Merchant;

import java.util.Optional;

/**
 * 精判：判断帖子是否真是该商家的潜在客户，生成中文摘要和越南语建议回复。
 * 返回 empty 表示不推送。
 */
public interface LeadJudge {
    Optional<Lead> judge(Post post, Merchant merchant);
}
