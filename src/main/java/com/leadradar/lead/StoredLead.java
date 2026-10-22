package com.leadradar.lead;

import java.time.Instant;

public record StoredLead(
        long id,
        String merchantId,
        String source,
        String postUrl,
        String postText,
        String summaryZh,
        String replyVi,
        int score,
        LeadStatus status,
        String note,
        Instant createdAt,
        Instant updatedAt
) {
}
