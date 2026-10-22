package com.leadradar.lead;

import com.leadradar.merchant.Merchant;

/**
 * 一条推给某个商家的线索。
 *
 * @param post           原帖
 * @param merchant       目标商家
 * @param summaryZh      中文摘要
 * @param suggestedReply 建议回复（越南语，老板确认后自己发）
 * @param score          匹配度 0-100
 */
public record Lead(Post post, Merchant merchant, String summaryZh, String suggestedReply, int score) {
}
