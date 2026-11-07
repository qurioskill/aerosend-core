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

    public record WindowCounts(long sent, long bounces, long replies) {}

    public record DomainSummary(long domainId, WindowCounts last14Days, WindowCounts last3Days,
                                Double latestPlacement, LocalDate latestPlacementDay, LocalDate lastDataDay) {}

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

    /**
     * One summary per domain the customer owns, over complete UTC days only (today is excluded).
     * The bounds are built from a UTC date, because subtracting an interval from a timestamptz
     * follows the session time zone and would move them across a DST change.
     */
    public List<DomainSummary> domainSummaries(long customerId) {
        return jdbc.query("""
            WITH utc AS (SELECT (now() AT TIME ZONE 'UTC')::date AS today),
            bounds AS (
                SELECT today::timestamp AT TIME ZONE 'UTC'        AS window_end,
                       (today - 14)::timestamp AT TIME ZONE 'UTC' AS from_14_days,
                       (today - 3)::timestamp AT TIME ZONE 'UTC'  AS from_3_days
                FROM utc
            )
            SELECT d.id,
                   coalesce(sum(s.sent), 0)::bigint,
                   coalesce(sum(s.bounces), 0)::bigint,
                   coalesce(sum(s.replies), 0)::bigint,
                   coalesce(sum(s.sent)    FILTER (WHERE s.day >= b.from_3_days), 0)::bigint,
                   coalesce(sum(s.bounces) FILTER (WHERE s.day >= b.from_3_days), 0)::bigint,
                   coalesce(sum(s.replies) FILTER (WHERE s.day >= b.from_3_days), 0)::bigint,
                   (array_agg(s.inbox_placement ORDER BY s.day DESC)
                       FILTER (WHERE s.inbox_placement IS NOT NULL))[1],
                   (max(s.day) FILTER (WHERE s.inbox_placement IS NOT NULL) AT TIME ZONE 'UTC')::date,
                   (max(s.day) AT TIME ZONE 'UTC')::date
            FROM domains d
            CROSS JOIN bounds b
            LEFT JOIN domain_daily_stats s
                   ON s.domain_id = d.id AND s.day >= b.from_14_days AND s.day < b.window_end
            WHERE d.customer_id = ?
            GROUP BY d.id
            ORDER BY d.id
            """,
            (rs, i) -> new DomainSummary(rs.getLong(1),
                new WindowCounts(rs.getLong(2), rs.getLong(3), rs.getLong(4)),
                new WindowCounts(rs.getLong(5), rs.getLong(6), rs.getLong(7)),
                (Double) rs.getObject(8),
                rs.getObject(9, LocalDate.class),
                rs.getObject(10, LocalDate.class)),
            customerId);
    }
}
