package com.leadradar.pipeline;

import com.leadradar.judge.LeadJudge;
import com.leadradar.lead.Lead;
import com.leadradar.lead.LeadRepository;
import com.leadradar.lead.LeadStatus;
import com.leadradar.lead.Post;
import com.leadradar.merchant.Merchant;
import com.leadradar.notify.Notifier;
import com.leadradar.source.ManualImportSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class RadarPipelineIT {

    static final List<String> sent = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class Stubs {
        /** 帖子里含 "LOW" 的给低分，含 "SPAM" 的判为无关，其余高分。 */
        @Bean
        @Primary
        LeadJudge stubJudge() {
            return (post, merchant) -> {
                if (post.text().contains("SPAM")) {
                    return Optional.empty();
                }
                int score = post.text().contains("LOW") ? 30 : 90;
                return Optional.of(new Lead(post, merchant, "摘要:" + post.sourceId(), "Dạ em chào anh", score));
            };
        }

        @Bean
        @Primary
        Notifier capturingNotifier() {
            return new Notifier() {
                @Override
                public void send(long leadId, Lead lead) {
                    sent.add("#" + leadId + " " + lead.merchant().id() + " " + lead.summaryZh());
                }

                @Override
                public void sendText(Merchant merchant, String text) {
                    sent.add("text " + merchant.id());
                }
            };
        }
    }

    @Autowired
    RadarPipeline pipeline;
    @Autowired
    ManualImportSource source;
    @Autowired
    LeadRepository repo;

    @BeforeEach
    void reset() {
        sent.clear();
    }

    private Post post(String id, String text) {
        return new Post("manual", id, "https://x/" + id, "a", text, Instant.now());
    }

    @Test
    void relevantPostIsStoredAndPushedOnce() {
        source.submit(post("p1", "Ai biết quán món Trung giao tới Sunrise City không?"));
        assertEquals(1, pipeline.run());
        assertEquals(1, sent.size());
        assertTrue(sent.get(0).contains("demo-chinese-restaurant-q7"));

        var stored = repo.find("demo-chinese-restaurant-q7", LeadStatus.NEW, 10);
        assertTrue(stored.stream().anyMatch(l -> "https://x/p1".equals(l.postUrl())));

        // 同一帖子再来一次不重复推送
        source.submit(post("p1", "Ai biết quán món Trung giao tới Sunrise City không?"));
        assertEquals(0, pipeline.run());
        assertEquals(1, sent.size());
    }

    @Test
    void nonMatchingLowScoreAndIrrelevantPostsAreNotPushed() {
        source.submit(post("p2", "Bán xe máy cũ giá rẻ"));                 // 关键词不命中
        source.submit(post("p3", "Tìm quán món Trung LOW"));               // 低于 min-score
        source.submit(post("p4", "Quán món Trung SPAM khai trương"));      // 模型判无关
        source.submit(post("p5", "Tìm quán món Trung ở Quận 3"));          // 区域不符
        assertEquals(0, pipeline.run());
        assertTrue(sent.isEmpty(), () -> new ArrayList<>(sent).toString());
    }
}
