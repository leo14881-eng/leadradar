package com.leadradar.brief;

import com.leadradar.lead.LeadRepository;
import com.leadradar.lead.LeadStatus;
import com.leadradar.merchant.Merchant;
import com.leadradar.merchant.MerchantProperties;
import com.leadradar.notify.Notifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

/**
 * 每日简报：今天找到几条线索、还有几条没跟进、本周成交几单。
 * 让老板每天都看到"这东西在帮我赚钱"，是续费的关键。
 */
@Component
public class DailyBriefJob {
    private final MerchantProperties merchants;
    private final LeadRepository repo;
    private final Notifier notifier;
    private final Clock clock;
    private final ZoneId zone;

    public DailyBriefJob(MerchantProperties merchants, LeadRepository repo, Notifier notifier, Clock clock,
                         @Value("${leadradar.brief.zone:Asia/Ho_Chi_Minh}") String zone) {
        this.merchants = merchants;
        this.repo = repo;
        this.notifier = notifier;
        this.clock = clock;
        this.zone = ZoneId.of(zone);
    }

    @Scheduled(cron = "${leadradar.brief.cron:0 0 21 * * *}", zone = "${leadradar.brief.zone:Asia/Ho_Chi_Minh}")
    public void sendAll() {
        for (var m : merchants.merchants()) {
            notifier.sendText(m, build(m));
        }
    }

    public String build(Merchant m) {
        var today = clock.instant().atZone(zone).toLocalDate();
        var dayStart = today.atStartOfDay(zone).toInstant();
        var dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant();
        var weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(zone).toInstant();

        int newToday = repo.countCreated(m.id(), dayStart, dayEnd);
        int pending = repo.countByStatus(m.id(), LeadStatus.NEW);
        int wonThisWeek = repo.countUpdatedByStatus(m.id(), weekStart, dayEnd).getOrDefault(LeadStatus.WON, 0);

        var sb = new StringBuilder();
        sb.append("📊 ").append(m.name()).append(" 今日简报（").append(today).append("）\n");
        sb.append("今天新线索：").append(newToday).append(" 条\n");
        sb.append("待跟进：").append(pending).append(" 条\n");
        sb.append("本周成交：").append(wonThisWeek).append(" 单");
        if (pending > 0) {
            sb.append("\n\n还有 ").append(pending).append(" 条线索没回复，客户等久了就去别家了。");
        }
        return sb.toString();
    }
}
