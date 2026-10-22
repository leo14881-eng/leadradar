package com.leadradar.pipeline;

import com.leadradar.filter.RuleFilter;
import com.leadradar.judge.LeadJudge;
import com.leadradar.judge.LlmProperties;
import com.leadradar.lead.LeadRepository;
import com.leadradar.merchant.MerchantProperties;
import com.leadradar.notify.Notifier;
import com.leadradar.source.PostSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * 主流程：采集 → 去重 → 规则粗筛 → LLM 精判 → 入库 → 推送。
 */
@Component
public class RadarPipeline {
    private static final Logger log = LoggerFactory.getLogger(RadarPipeline.class);

    private final List<PostSource> sources;
    private final MerchantProperties merchants;
    private final RuleFilter filter;
    private final LeadJudge judge;
    private final LeadRepository repo;
    private final Notifier notifier;
    private final int minScore;
    private final Clock clock;
    private volatile Instant lastRun;

    public RadarPipeline(List<PostSource> sources, MerchantProperties merchants, RuleFilter filter, LeadJudge judge,
                         LeadRepository repo, Notifier notifier, LlmProperties llm, Clock clock) {
        this.sources = sources;
        this.merchants = merchants;
        this.filter = filter;
        this.judge = judge;
        this.repo = repo;
        this.notifier = notifier;
        this.minScore = llm.minScore();
        this.clock = clock;
        this.lastRun = clock.instant().minusSeconds(3600);
    }

    @Scheduled(fixedDelayString = "${leadradar.poll-interval:PT1M}")
    public synchronized int run() {
        var since = lastRun;
        lastRun = clock.instant();
        int pushed = 0;
        for (var source : sources) {
            List<com.leadradar.lead.Post> posts;
            try {
                posts = source.fetchSince(since);
            } catch (RuntimeException e) {
                log.error("Source {} failed: {}", source.name(), e.toString());
                continue;
            }
            for (var post : posts) {
                if (!repo.markSeen(post.dedupKey())) {
                    continue;
                }
                for (var merchant : merchants.merchants()) {
                    if (!filter.matches(post, merchant)) {
                        continue;
                    }
                    var lead = judge.judge(post, merchant);
                    if (lead.isEmpty() || (lead.get().modelScored() && lead.get().score() < minScore)) {
                        log.debug("Dropped {} for {}", post.dedupKey(), merchant.id());
                        continue;
                    }
                    long id = repo.save(lead.get());
                    log.info("lead #{} {} -> {} (score {})", id, post.dedupKey(), merchant.id(), lead.get().score());
                    notifier.send(id, lead.get());
                    pushed++;
                }
            }
        }
        return pushed;
    }
}
