package com.leadradar.notify;

import com.leadradar.lead.Lead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * 推送到老板的 Telegram。未配置 bot token 时只打日志，方便本地调试。
 */
@Component
public class TelegramNotifier implements Notifier {
    private static final Logger log = LoggerFactory.getLogger(TelegramNotifier.class);

    private final String botToken;
    private final RestClient http = RestClient.create("https://api.telegram.org");

    public TelegramNotifier(@Value("${leadradar.telegram.bot-token:}") String botToken) {
        this.botToken = botToken;
    }

    @Override
    public void send(Lead lead) {
        var msg = format(lead);
        if (botToken.isBlank() || lead.merchant().telegramChatId() == null) {
            log.info("[dry-run] -> {}\n{}", lead.merchant().name(), msg);
            return;
        }
        http.post()
                .uri("/bot{token}/sendMessage", botToken)
                .body(Map.of("chat_id", lead.merchant().telegramChatId(), "text", msg, "disable_web_page_preview", true))
                .retrieve()
                .toBodilessEntity();
    }

    static String format(Lead lead) {
        var p = lead.post();
        return """
                🎯 新线索（匹配度 %d）
                %s

                建议回复：
                %s

                原帖：%s
                """.formatted(lead.score(), lead.summaryZh(), lead.suggestedReply(), p.url() == null ? "-" : p.url());
    }
}
