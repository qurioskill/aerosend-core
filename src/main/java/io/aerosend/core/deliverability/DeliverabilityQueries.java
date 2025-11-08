package io.aerosend.core.deliverability;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class DeliverabilityQueries {

    public record DailyStats(LocalDate day, long sent, long bounces, long replies,
                             Double warmupScore, Double inboxPlacement) {
        public double bounceRate() { return sent == 0 ? 0 : (double) bounces / sent; }
        public double replyRate()  { return sent == 0 ? 0 : (double) replies / sent; }
    }

    private final JdbcTemplate jdbc;

    public DeliverabilityQueries(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** Reads from the domain_daily_stats continuous aggregate (TimescaleDB). */
    public List<DailyStats> dailyStats(long domainId, int days) {
        return jdbc.query("""
            SELECT day::date, sent, bounces, replies, warmup_score, inbox_placement
            FROM domain_daily_stats
            WHERE domain_id = ? AND day > now() - make_interval(days => ?)
            ORDER BY day
            """,
            (rs, i) -> new DailyStats(rs.getDate(1).toLocalDate(), rs.getLong(2), rs.getLong(3), rs.getLong(4),
                (Double) rs.getObject(5), (Double) rs.getObject(6)),
            domainId, days);
    }
}
