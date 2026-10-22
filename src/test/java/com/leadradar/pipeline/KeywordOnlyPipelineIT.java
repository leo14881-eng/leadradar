package com.leadradar.pipeline;

import com.leadradar.lead.Lead;
import com.leadradar.lead.Post;
import com.leadradar.merchant.Merchant;
import com.leadradar.notify.Notifier;
import com.leadradar.source.ManualImportSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 用真实的默认配置（provider=none，不替换 judge）：关键词命中必须能推出去，不能被 min-score 拦掉。 */
@SpringBootTest
@ActiveProfiles("test")
class KeywordOnlyPipelineIT {
    static final List<String> sent = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class Capture {
        @Bean
        @Primary
        Notifier capture() {
            return new Notifier() {
                @Override
                public void send(long leadId, Lead lead) {
                    sent.add(com.leadradar.notify.TelegramNotifierAccess.format(leadId, lead));
                }

                @Override
                public void sendText(Merchant merchant, String text) {
                }
            };
        }
    }

    @Autowired
    RadarPipeline pipeline;
    @Autowired
    ManualImportSource source;

    @Test
    void keywordMatchIsPushedWithoutModel() {
        source.submit(new Post("manual", "k1", "https://x/k1", "a",
                "Tối nay muốn đặt món Trung giao tới Sunrise City", Instant.now()));
        assertEquals(1, pipeline.run());
        assertTrue(sent.get(0).contains("关键词命中"), sent.get(0));
    }
}
