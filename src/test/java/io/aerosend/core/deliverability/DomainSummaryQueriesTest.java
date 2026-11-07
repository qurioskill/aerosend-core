package io.aerosend.core.deliverability;

import io.aerosend.core.deliverability.DeliverabilityQueries.DomainSummary;
import io.aerosend.core.deliverability.DeliverabilityQueries.WindowCounts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs Flyway V1 to V3 on a clean TimescaleDB, so a failing migration fails every test here.
 * Needs Docker: there is deliberately no disabledWithoutDocker, so a missing Docker fails the build.
 */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(DeliverabilityQueries.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED) // refresh_continuous_aggregate cannot run in a transaction
@Testcontainers
class DomainSummaryQueriesTest {

    private static final LocalTime LAST_SECOND = LocalTime.of(23, 59, 59);
    private static final AtomicLong NEXT_CUSTOMER = new AtomicLong(1000);

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
        DockerImageName.parse("timescale/timescaledb:2.15.2-pg16").asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // A session far from UTC, so the windows and dates are shown not to depend on the session time zone.
        registry.add("spring.datasource.hikari.connection-init-sql", () -> "SET TIME ZONE 'Pacific/Auckland'");
    }

    @Autowired DeliverabilityQueries queries;
    @Autowired JdbcTemplate jdbc;

    private final LocalDate today = LocalDate.now(ZoneOffset.UTC);
    private final long customer = NEXT_CUSTOMER.incrementAndGet();

    @Test
    void flywayAppliesEveryMigrationOnACleanDatabase() {
        assertThat(jdbc.queryForList(
            "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class))
            .containsExactly("1", "2", "3");
    }

    @Test
    void eventTodayIsExcludedFromBothWindows() {
        long domain = domain("today");
        event(domain, at(today, LocalTime.MIDNIGHT), "SENT", null);
        event(domain, at(today.minusDays(1), LAST_SECOND), "SENT", null);
        refresh();

        assertThat(jdbc.queryForObject("""
            SELECT count(*) FROM domain_daily_stats
            WHERE domain_id = ? AND day = ?
            """, Long.class, domain, at(today, LocalTime.MIDNIGHT)))
            .as("today must be in the rollup, or this test proves nothing")
            .isEqualTo(1);

        DomainSummary summary = onlySummary();
        assertThat(summary.last14Days().sent()).isEqualTo(1);
        assertThat(summary.last3Days().sent()).isEqualTo(1);
        assertThat(summary.lastDataDay()).isEqualTo(today.minusDays(1));
    }

    @Test
    void fourteenDaysAgoCountsAndFifteenDoesNot() {
        long domain = domain("edge14");
        event(domain, at(today.minusDays(14), LocalTime.MIDNIGHT), "SENT", null);
        event(domain, at(today.minusDays(15), LAST_SECOND), "SENT", null);
        refresh();

        DomainSummary summary = onlySummary();
        assertThat(summary.last14Days().sent()).isEqualTo(1);
        assertThat(summary.last3Days().sent()).isZero();
        assertThat(summary.lastDataDay()).isEqualTo(today.minusDays(14));
    }

    @Test
    void threeDaysAgoCountsAndFourDoesNot() {
        long domain = domain("edge3");
        event(domain, at(today.minusDays(3), LocalTime.MIDNIGHT), "SENT", null);
        event(domain, at(today.minusDays(4), LAST_SECOND), "SENT", null);
        refresh();

        DomainSummary summary = onlySummary();
        assertThat(summary.last3Days().sent()).isEqualTo(1);
        assertThat(summary.last14Days().sent()).isEqualTo(2);
    }

    @Test
    void eachWindowSumsItsCountsAcrossDays() {
        long domain = domain("sums");
        for (int day : new int[] {1, 2, 6}) {
            OffsetDateTime time = at(today.minusDays(day), LocalTime.NOON);
            events(domain, time, "SENT", 4);
            events(domain, time, "BOUNCE", 2);
            events(domain, time, "REPLY", 1);
        }
        refresh();

        DomainSummary summary = onlySummary();
        assertThat(summary.last14Days()).isEqualTo(new WindowCounts(12, 6, 3));
        assertThat(summary.last3Days()).isEqualTo(new WindowCounts(8, 4, 2));
    }

    @Test
    void silentDomainReturnsZeroCountsAndNullDates() {
        long domain = domain("silent");
        refresh();

        assertThat(queries.domainSummaries(customer)).containsExactly(new DomainSummary(
            domain, new WindowCounts(0, 0, 0), new WindowCounts(0, 0, 0), null, null, null));
    }

    @Test
    void bouncesWithNoSendsAreReturnedAsTheyAre() {
        long domain = domain("nosends");
        events(domain, at(today.minusDays(2), LocalTime.NOON), "BOUNCE", 3);
        refresh();

        DomainSummary summary = onlySummary();
        assertThat(summary.last14Days()).isEqualTo(new WindowCounts(0, 3, 0));
        assertThat(summary.last3Days()).isEqualTo(new WindowCounts(0, 3, 0));
    }

    @Test
    void latestPlacementIsTheMostRecentNonNullValue() {
        long domain = domain("placement");
        event(domain, at(today.minusDays(6), LocalTime.NOON), "PLACEMENT", 0.5);
        event(domain, at(today.minusDays(3), LocalTime.NOON), "PLACEMENT", 0.9);
        event(domain, at(today.minusDays(1), LocalTime.NOON), "SENT", null);
        refresh();

        DomainSummary summary = onlySummary();
        assertThat(summary.latestPlacement()).isEqualTo(0.9);
        assertThat(summary.latestPlacementDay()).isEqualTo(today.minusDays(3));
        assertThat(summary.lastDataDay()).isEqualTo(today.minusDays(1));
    }

    @Test
    void latestPlacementIsNullWhenNoneIsInTheWindow() {
        long domain = domain("oldplacement");
        event(domain, at(today.minusDays(20), LocalTime.NOON), "PLACEMENT", 0.5);
        event(domain, at(today.minusDays(1), LocalTime.NOON), "SENT", null);
        refresh();

        DomainSummary summary = onlySummary();
        assertThat(summary.latestPlacement()).isNull();
        assertThat(summary.latestPlacementDay()).isNull();
        assertThat(summary.lastDataDay()).isEqualTo(today.minusDays(1));
    }

    @Test
    void anotherCustomersDomainIsNotReturned() {
        long mine = domain("mine");
        long theirs = insertDomain(NEXT_CUSTOMER.incrementAndGet(), "theirs");
        event(mine, at(today.minusDays(1), LocalTime.NOON), "SENT", null);
        event(theirs, at(today.minusDays(1), LocalTime.NOON), "SENT", null);
        refresh();

        assertThat(queries.domainSummaries(customer))
            .extracting(DomainSummary::domainId)
            .containsExactly(mine);
    }

    private DomainSummary onlySummary() {
        List<DomainSummary> summaries = queries.domainSummaries(customer);
        assertThat(summaries).hasSize(1);
        return summaries.get(0);
    }

    private long domain(String label) {
        return insertDomain(customer, label);
    }

    private long insertDomain(long customerId, String label) {
        Long batch = jdbc.queryForObject(
            "INSERT INTO server_batches (customer_id, region) VALUES (?, 'test') RETURNING id", Long.class, customerId);
        return jdbc.queryForObject(
            "INSERT INTO domains (customer_id, server_batch_id, name) VALUES (?, ?, ?) RETURNING id",
            Long.class, customerId, batch, label + "-" + customerId + ".example");
    }

    private void events(long domainId, OffsetDateTime time, String kind, int count) {
        for (int i = 0; i < count; i++) {
            event(domainId, time, kind, null);
        }
    }

    private void event(long domainId, OffsetDateTime time, String kind, Double value) {
        jdbc.update("INSERT INTO deliverability_events (time, domain_id, kind, value) VALUES (?, ?, ?, ?)",
            time, domainId, kind, value);
    }

    private void refresh() {
        jdbc.update("CALL refresh_continuous_aggregate('domain_daily_stats', ?, ?)",
            at(today.minusDays(30), LocalTime.MIDNIGHT), at(today.plusDays(1), LocalTime.MIDNIGHT));
    }

    private static OffsetDateTime at(LocalDate day, LocalTime time) {
        return day.atTime(time).atOffset(ZoneOffset.UTC);
    }
}
