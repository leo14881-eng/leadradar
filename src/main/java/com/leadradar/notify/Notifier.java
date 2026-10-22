package com.leadradar.notify;

import com.leadradar.lead.Lead;

public interface Notifier {
    void send(Lead lead);
}
