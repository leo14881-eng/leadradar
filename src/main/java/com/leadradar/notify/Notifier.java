package com.leadradar.notify;

import com.leadradar.lead.Lead;
import com.leadradar.merchant.Merchant;

public interface Notifier {
    /** 推送一条线索，leadId 用于老板回报跟进结果。 */
    void send(long leadId, Lead lead);

    /** 推送任意文本（每日简报等）。 */
    void sendText(Merchant merchant, String text);
}
