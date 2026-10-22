package com.leadradar.filter;

import com.leadradar.lead.Post;
import com.leadradar.merchant.Merchant;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleFilterTest {
    private final RuleFilter filter = new RuleFilter();
    private final Merchant merchant = new Merchant("m1", "店", "中餐厅",
            List.of("Quận 7"), List.of("món trung"), List.of("tuyển dụng"), null, "");

    private Post post(String text) {
        return new Post("t", "1", null, "a", text, Instant.now());
    }

    @Test
    void matchesKeywordWithoutDiacritics() {
        assertTrue(filter.matches(post("Ai biết chỗ nào bán mon trung ngon ở quan 7 không?"), merchant));
    }

    @Test
    void rejectsOtherDistrict() {
        assertFalse(filter.matches(post("Tìm quán món Trung ở Quận 3"), merchant));
    }

    @Test
    void rejectsExcludedKeyword() {
        assertFalse(filter.matches(post("Tuyển dụng phụ bếp món Trung Quận 7"), merchant));
    }

    @Test
    void passesWhenNoDistrictMentioned() {
        assertTrue(filter.matches(post("Có ai giao món Trung tối nay không?"), merchant));
    }

    @Test
    void quanMeaningShopIsNotADistrict() {
        assertTrue(filter.matches(post("Tìm quán món Trung ngon giao tận nhà"), merchant));
    }
}
