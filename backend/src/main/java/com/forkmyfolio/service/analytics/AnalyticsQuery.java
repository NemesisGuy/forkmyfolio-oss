package com.forkmyfolio.service.analytics;

import lombok.Getter;

/**
 * A validated analytics query (date range). Constructed exclusively through
 * {@link #of(Long, Long, String)} which enforces the validation policy:
 * either a whitelisted relative period or an explicit epoch-millisecond range
 * (end &gt; start, span &le; 366 days). Never constructed from raw browser input
 * without validation.
 */
@Getter
public final class AnalyticsQuery {

    /** Whitelisted relative periods accepted from clients. */
    private static final java.util.Set<String> ALLOWED_PERIODS =
            java.util.Set.of("24h", "7d", "30d", "90d", "12month");

    private static final long MAX_RANGE_MS = 366L * 24 * 60 * 60 * 1000;

    private final String period;   // nullable
    private final Long startAt;    // epoch ms, nullable
    private final Long endAt;      // epoch ms, nullable

    private AnalyticsQuery(String period, Long startAt, Long endAt) {
        this.period = period;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    /**
     * Validates and builds a query. Throws {@link IllegalArgumentException}
     * (mapped to HTTP 400) on invalid input.
     */
    public static AnalyticsQuery of(Long startAt, Long endAt, String period) {
        boolean hasPeriod = period != null && !period.isBlank();
        boolean hasRange = startAt != null || endAt != null;

        if (hasPeriod && hasRange) {
            throw new IllegalArgumentException("Provide either a period or an explicit startAt/endAt range, not both.");
        }
        if (hasPeriod) {
            String p = period.trim();
            if (!ALLOWED_PERIODS.contains(p)) {
                throw new IllegalArgumentException("Unsupported period. Allowed: " + ALLOWED_PERIODS);
            }
            return new AnalyticsQuery(p, null, null);
        }
        if (!hasRange) {
            // Default to the last 7 days when nothing is supplied.
            return new AnalyticsQuery("7d", null, null);
        }
        if (startAt == null || endAt == null) {
            throw new IllegalArgumentException("Both startAt and endAt are required for an explicit range.");
        }
        if (startAt <= 0 || endAt <= 0) {
            throw new IllegalArgumentException("startAt and endAt must be positive epoch milliseconds.");
        }
        if (endAt <= startAt) {
            throw new IllegalArgumentException("endAt must be after startAt.");
        }
        if (endAt - startAt > MAX_RANGE_MS) {
            throw new IllegalArgumentException("Date range must not exceed 366 days.");
        }
        if (startAt > System.currentTimeMillis() + 60_000L) {
            throw new IllegalArgumentException("startAt must not be in the future.");
        }
        return new AnalyticsQuery(null, startAt, endAt);
    }

    /** Resolves the concrete epoch range (used for caching keys and provider calls). */
    public long resolveStartAt() {
        if (startAt != null) return startAt;
        long now = System.currentTimeMillis();
        return switch (period) {
            case "24h" -> now - 24L * 60 * 60 * 1000;
            case "7d" -> now - 7L * 24 * 60 * 60 * 1000;
            case "30d" -> now - 30L * 24 * 60 * 60 * 1000;
            case "90d" -> now - 90L * 24 * 60 * 60 * 1000;
            case "12month" -> now - 365L * 24 * 60 * 60 * 1000;
            default -> now - 7L * 24 * 60 * 60 * 1000;
        };
    }

    public long resolveEndAt() {
        return endAt != null ? endAt : System.currentTimeMillis();
    }
}
