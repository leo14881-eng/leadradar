package com.leadradar.notify;

import com.leadradar.lead.Lead;

/** 测试用：访问包内的消息格式化方法。 */
public final class TelegramNotifierAccess {
    private TelegramNotifierAccess() {
    }

    public static String format(long id, Lead lead) {
        return TelegramNotifier.format(id, lead);
    }
}
