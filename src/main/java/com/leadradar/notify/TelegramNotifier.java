package com.leadradar.notify;

import com.leadradar.lead.Lead;
import com.leadradar.merchant.Merchant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * 推送到老板的 Telegram。未配置 bot token 或商家没填 chat id 时只打日志（dry-run）。
 */
@Component
public class TelegramNotifier implements Notifier {
    private static final Logger log = LoggerFactory.getLogger(TelegramNotifier.class);

    private final String botToken;
    private final RestClient http;

    public TelegramNotifier(@Value("${leadradar.telegram.bot-token:}") String botToken,
                            RestClient.Builder builder) {
        this.botToken = botToken;
        this.http = builder.clone().baseUrl("https://api.telegram.org").build();
    }

    @Override
    public void send(long leadId, Lead lead) {
        sendText(lead.merchant(), format(leadId, lead));
    }

    @Override
    public void sendText(Merchant merchant, String text) {
        var chatId = merchant.telegramChatId();
        if (botToken.isBlank() || chatId == null || chatId.isBlank()) {
            log.info("[dry-run] -> {}\n{}", merchant.name(), text);
            return;
        }
        try {
            http.post()
                    .uri("/bot{token}/sendMessage", botToken)
                    .body(Map.of("chat_id", chatId, "text", text, "disable_web_page_preview", true))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            // 推送失败不能拖垮整条流水线；线索已入库，可在 /api/leads 查到
            log.error("Telegram send failed for merchant {}: {}", merchant.id(), e.toString());
        }
    }

    static String format(long leadId, Lead lead) {
        var p = lead.post();
        var scoreText = lead.modelScored() ? "匹配度 " + lead.score() : "关键词命中";
        return """
                🎯 新线索 #%d（%s）
                %s

                建议回复（越南语，可直接复制）：
                %s

                原帖：%s""".formatted(leadId, scoreText, lead.summaryZh(), lead.suggestedReply(),
                p.url() == null ? "-" : p.url());
    }
}
