package com.leadradar.lead;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class LeadRepository {
    private final JdbcClient jdbc;
    private final Clock clock;

    public LeadRepository(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** 标记帖子已处理；第一次见到返回 true，重复返回 false。 */
    public boolean markSeen(String dedupKey) {
        try {
            jdbc.sql("INSERT INTO seen_posts (dedup_key, seen_at) VALUES (:k, :t)")
                    .param("k", dedupKey)
                    .param("t", Timestamp.from(clock.instant()))
                    .update();
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    public long save(Lead lead) {
        var now = Timestamp.from(clock.instant());
        var keys = new GeneratedKeyHolder();
        jdbc.sql("""
                        INSERT INTO leads (merchant_id, source, post_url, post_text, summary_zh, reply_vi, score,
                                           status, created_at, updated_at)
                        VALUES (:m, :s, :u, :t, :sum, :r, :score, :st, :now, :now)
                        """)
                .param("m", lead.merchant().id())
                .param("s", lead.post().source())
                .param("u", lead.post().url())
                .param("t", lead.post().text())
                .param("sum", lead.summaryZh())
                .param("r", lead.suggestedReply())
                .param("score", lead.score())
                .param("st", LeadStatus.NEW.name())
                .param("now", now)
                .update(keys, "id");
        return keys.getKey().longValue();
    }

    public Optional<StoredLead> findById(long id) {
        return jdbc.sql("SELECT * FROM leads WHERE id = :id").param("id", id).query(LeadRepository::map).optional();
    }

    public List<StoredLead> find(String merchantId, LeadStatus status, int limit) {
        var sql = new StringBuilder("SELECT * FROM leads WHERE 1=1");
        var params = new java.util.HashMap<String, Object>();
        if (merchantId != null) {
            sql.append(" AND merchant_id = :m");
            params.put("m", merchantId);
        }
        if (status != null) {
            sql.append(" AND status = :st");
            params.put("st", status.name());
        }
        sql.append(" ORDER BY created_at DESC, id DESC LIMIT :limit");
        params.put("limit", limit);
        return jdbc.sql(sql.toString()).params(params).query(LeadRepository::map).list();
    }

    public boolean updateStatus(long id, LeadStatus status, String note) {
        return jdbc.sql("UPDATE leads SET status = :st, note = COALESCE(:note, note), updated_at = :now WHERE id = :id")
                .param("st", status.name())
                .param("note", note)
                .param("now", Timestamp.from(clock.instant()))
                .param("id", id)
                .update() == 1;
    }

    /** 某商家在 [from, to) 时间段内新增线索数。 */
    public int countCreated(String merchantId, Instant from, Instant to) {
        return jdbc.sql("SELECT COUNT(*) FROM leads WHERE merchant_id = :m AND created_at >= :f AND created_at < :t")
                .param("m", merchantId).param("f", Timestamp.from(from)).param("t", Timestamp.from(to))
                .query(Integer.class).single();
    }

    /** 某商家在 [from, to) 时间段内被更新为各状态的线索数（用于统计成交）。 */
    public Map<LeadStatus, Integer> countUpdatedByStatus(String merchantId, Instant from, Instant to) {
        return jdbc.sql("""
                        SELECT status, COUNT(*) AS c FROM leads
                        WHERE merchant_id = :m AND updated_at >= :f AND updated_at < :t
                        GROUP BY status""")
                .param("m", merchantId).param("f", Timestamp.from(from)).param("t", Timestamp.from(to))
                .query((rs, n) -> Map.entry(LeadStatus.valueOf(rs.getString("status")), rs.getInt("c")))
                .list().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public int countByStatus(String merchantId, LeadStatus status) {
        return jdbc.sql("SELECT COUNT(*) FROM leads WHERE merchant_id = :m AND status = :st")
                .param("m", merchantId).param("st", status.name())
                .query(Integer.class).single();
    }

    private static StoredLead map(ResultSet rs, int n) throws SQLException {
        return new StoredLead(
                rs.getLong("id"),
                rs.getString("merchant_id"),
                rs.getString("source"),
                rs.getString("post_url"),
                rs.getString("post_text"),
                rs.getString("summary_zh"),
                rs.getString("reply_vi"),
                rs.getInt("score"),
                LeadStatus.valueOf(rs.getString("status")),
                rs.getString("note"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }
}
