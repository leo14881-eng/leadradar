package com.leadradar.filter;

import com.leadradar.lead.Post;
import com.leadradar.merchant.Merchant;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 规则粗筛：先丢掉 90% 不相关的帖子，只把候选交给 LLM，控制模型费用。
 * 越南语去声调后再匹配，"quận 7" 和 "quan 7" 都能命中。
 */
@Component
public class RuleFilter {
    /** 区域写法：quan 7 / q7 / q.7 / district 7 / 7郡；注意 "quán"（店）去声调后也是 quan，必须带数字 */
    private static final Pattern DISTRICT =
            Pattern.compile("\\b(quan|q\\.?|district)\\s*\\d{1,2}\\b|\\d{1,2}\\s*郡");

    public boolean matches(Post post, Merchant merchant) {
        if (post.text() == null || post.text().isBlank()) {
            return false;
        }
        var text = normalize(post.text());
        for (var ex : merchant.excludeKeywords()) {
            if (text.contains(normalize(ex))) {
                return false;
            }
        }
        boolean keywordHit = merchant.keywords().stream().anyMatch(k -> text.contains(normalize(k)));
        if (!keywordHit) {
            return false;
        }
        if (merchant.districts().isEmpty()) {
            return true;
        }
        // 帖子没写区域也放行，交给 LLM 判断；写了别的区域则丢弃
        boolean mentionsAnyDistrict = DISTRICT.matcher(text).find();
        return !mentionsAnyDistrict || merchant.districts().stream().anyMatch(d -> text.contains(normalize(d)));
    }

    static String normalize(String s) {
        var lower = s.toLowerCase(Locale.ROOT).replace('đ', 'd');
        return Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }
}
