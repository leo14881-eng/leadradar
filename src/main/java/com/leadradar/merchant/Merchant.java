package com.leadradar.merchant;

import java.util.List;

/**
 * 一个付费（或试用）商家的监控配置。验证期由人工在 application.yml 里配置，不做后台。
 *
 * @param id              商家标识
 * @param name            店名
 * @param category        品类，如 中餐厅 / 民宿 / 房产中介
 * @param districts       服务区域，如 Quận 7、Quận 1
 * @param keywords        命中关键词（中文、越南语都写），任一命中即进入候选
 * @param excludeKeywords 排除词，命中则丢弃（如 "pass lại" 转让、广告帖）
 * @param telegramChatId  推送给老板的 Telegram chat id
 * @param pitch           店铺卖点和价格要点，用于生成建议回复
 */
public record Merchant(
        String id,
        String name,
        String category,
        List<String> districts,
        List<String> keywords,
        List<String> excludeKeywords,
        String telegramChatId,
        String pitch
) {
    public Merchant {
        districts = districts == null ? List.of() : List.copyOf(districts);
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
        excludeKeywords = excludeKeywords == null ? List.of() : List.copyOf(excludeKeywords);
    }
}
