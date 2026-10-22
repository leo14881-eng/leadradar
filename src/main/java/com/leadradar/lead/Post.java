package com.leadradar.lead;

import java.time.Instant;

/**
 * 从任一数据源抓到的原始帖子。
 *
 * @param source   来源，如 facebook-group、chotot
 * @param sourceId 来源内唯一 id，用于去重
 * @param url      原帖链接
 * @param author   发帖人显示名（只用于展示，不入长期存储）
 * @param text     帖子正文（多为越南语）
 * @param postedAt 发帖时间
 */
public record Post(String source, String sourceId, String url, String author, String text, Instant postedAt) {
    public String dedupKey() {
        return source + ":" + sourceId;
    }
}
