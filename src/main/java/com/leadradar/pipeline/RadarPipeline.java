package com.leadradar.pipeline;

import com.leadradar.filter.RuleFilter;
import com.leadradar.judge.LeadJudge;
import com.leadradar.merchant.MerchantProperties;
import com.leadradar.notify.Notifier;
import com.leadradar.source.PostSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 主流程：采集 → 去重 → 规则粗筛 → LLM 精判 → 推送。
 */
@Component
public class RadarPipeline {
    private static final Logger log = LoggerFactory.getLogger(RadarPipeline.class);

    private final List<PostSource> sources;
    private final MerchantProperties props;
    private final RuleFilter filter;
    private final LeadJudge judge;
    private final Notifier notifier;
    private final Set<String> seen = ConcurrentHashMap.newKeySet(); // TODO: 换成 PostgreSQL 持久化
    private volatile Instant lastRun = Instant.now().minusSeconds(3600);

    public RadarPipeline(List<PostSource> sources, MerchantProperties props, RuleFilter filter,
                         LeadJudge judge, Notifier notifier) {
        this.sources = sources;
        this.props = props;
        this.filter = filter;
        this.judge = judge;
        this.notifier = notifier;
    }

    @Scheduled(fixedDelayString = "${leadradar.poll-interval:PT1M}")
    public void run() {
        var since = lastRun;
        lastRun = Instant.now();
        for (var source : sources) {
            for (var post : source.fetchSince(since)) {
                if (!seen.add(post.dedupKey())) {
                    continue;
                }
                for (var merchant : props.merchants()) {
                    if (filter.matches(post, merchant)) {
                        judge.judge(post, merchant).ifPresent(lead -> {
                            log.info("lead {} -> {}", post.dedupKey(), merchant.id());
                            notifier.send(lead);
                        });
                    }
                }
            }
        }
    }
}
