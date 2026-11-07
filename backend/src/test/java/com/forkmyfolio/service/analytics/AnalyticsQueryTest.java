package com.forkmyfolio.service.analytics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 8.5 (DEF-1) — {@link AnalyticsQuery} validation policy.
 *
 * <p>The query object is the single choke point between raw request
 * parameters and provider calls: whitelisted relative periods OR an explicit
 * epoch range (bounded span, sane ordering). Anything else is rejected before
 * a URL is ever built.</p>
 */
class AnalyticsQueryTest {

    private static final long HOUR = 60L * 60 * 1000;
    private static final long DAY = 24 * HOUR;

    @Test
    void noInput_defaultsToLastSevenDays() {
        AnalyticsQuery query = AnalyticsQuery.of(null, null, null);

        assertThat(query.getPeriod()).isEqualTo("7d");
        assertThat(query.resolveEndAt() - query.resolveStartAt()).isCloseTo(7 * DAY, org.assertj.core.data.Offset.offset(HOUR));
    }

    @Test
    void whitelistedPeriods_areAccepted() {
        for (String period : new String[]{"24h", "7d", "30d", "90d", "12month"}) {
            assertThat(AnalyticsQuery.of(null, null, period).getPeriod()).isEqualTo(period);
        }
    }

    @Test
    void unknownPeriod_isRejected() {
        assertThatThrownBy(() -> AnalyticsQuery.of(null, null, "13months"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AnalyticsQuery.of(null, null, "'; DROP TABLE users; --"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void periodAndExplicitRangeTogether_isRejected() {
        long start = 1_700_000_000_000L;
        assertThatThrownBy(() -> AnalyticsQuery.of(start, start + DAY, "7d"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void explicitRange_isAcceptedAndPreserved() {
        long start = 1_700_000_000_000L;
        long end = start + 30 * DAY;

        AnalyticsQuery query = AnalyticsQuery.of(start, end, null);

        assertThat(query.getPeriod()).isNull();
        assertThat(query.resolveStartAt()).isEqualTo(start);
        assertThat(query.resolveEndAt()).isEqualTo(end);
    }

    @Test
    void invertedRange_isRejected() {
        long start = 1_700_000_000_000L;
        assertThatThrownBy(() -> AnalyticsQuery.of(start + DAY, start, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("after");
    }

    @Test
    void rangeBeyond366Days_isRejected() {
        long start = 1_700_000_000_000L;
        assertThatThrownBy(() -> AnalyticsQuery.of(start, start + 367L * DAY, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("366");
    }

    @Test
    void halfOpenRange_isRejected() {
        long start = 1_700_000_000_000L;
        assertThatThrownBy(() -> AnalyticsQuery.of(start, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AnalyticsQuery.of(null, start, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nonPositiveTimestamps_areRejected() {
        assertThatThrownBy(() -> AnalyticsQuery.of(0L, 1000L, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AnalyticsQuery.of(-5L, 1000L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void futureStart_isRejected() {
        long future = System.currentTimeMillis() + 10L * DAY;
        assertThatThrownBy(() -> AnalyticsQuery.of(future, future + DAY, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");
    }

    @Test
    void blankPeriod_isTreatedAsAbsent() {
        assertThat(AnalyticsQuery.of(null, null, "   ").getPeriod()).isEqualTo("7d");
    }
}
