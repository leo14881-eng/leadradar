package com.leadradar.brief;

import com.leadradar.lead.LeadRepository;
import com.leadradar.lead.LeadStatus;
import com.leadradar.merchant.Merchant;
import com.leadradar.merchant.MerchantProperties;
import com.leadradar.notify.Notifier;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DailyBriefJobTest {
    @Test
    void briefUsesLocalDayBoundariesAndCounts() {
        var m = new Merchant("m1", "川味馆", "中餐厅", List.of(), List.of(), List.of(), null, "");
        var repo = mock(LeadRepository.class);
        // 2026-10-22 14:00 UTC = 21:00 胡志明时间
        var clock = Clock.fixed(Instant.parse("2026-10-22T14:00:00Z"), ZoneOffset.UTC);
        var dayStart = Instant.parse("2026-10-21T17:00:00Z");
        var dayEnd = Instant.parse("2026-10-22T17:00:00Z");
        var weekStart = Instant.parse("2026-10-18T17:00:00Z"); // 周一 00:00 胡志明时间

        when(repo.countCreated("m1", dayStart, dayEnd)).thenReturn(5);
        when(repo.countByStatus("m1", LeadStatus.NEW)).thenReturn(2);
        when(repo.countUpdatedByStatus(eq("m1"), eq(weekStart), eq(dayEnd))).thenReturn(Map.of(LeadStatus.WON, 3));

        var job = new DailyBriefJob(new MerchantProperties(List.of(m)), repo, mock(Notifier.class), clock,
                "Asia/Ho_Chi_Minh");
        var text = job.build(m);

        assertTrue(text.contains("2026-10-22"), text);
        assertTrue(text.contains("今天新线索：5 条"), text);
        assertTrue(text.contains("待跟进：2 条"), text);
        assertTrue(text.contains("本周成交：3 单"), text);
        assertTrue(text.contains("还有 2 条线索没回复"), text);
    }
}
