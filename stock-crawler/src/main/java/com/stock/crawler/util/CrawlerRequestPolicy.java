package com.stock.crawler.util;

import java.time.Duration;
import java.util.Objects;

/** Defines bounded timeout and retry behavior for one crawler HTTP operation. */
public record CrawlerRequestPolicy(
        Duration connectTimeout,
        Duration readTimeout,
        Duration callDeadline,
        int maxAttempts) {

    private static final CrawlerRequestPolicy LEGACY_DEFAULT = new CrawlerRequestPolicy(
            Duration.ofSeconds(10),
            Duration.ofSeconds(15),
            Duration.ofSeconds(50),
            3);

    private static final CrawlerRequestPolicy BACKGROUND_NEWS = new CrawlerRequestPolicy(
            Duration.ofSeconds(3),
            Duration.ofSeconds(8),
            Duration.ofSeconds(15),
            2);

    private static final CrawlerRequestPolicy INTERACTIVE = new CrawlerRequestPolicy(
            Duration.ofSeconds(2),
            Duration.ofSeconds(3),
            Duration.ofSeconds(5),
            1);

    public CrawlerRequestPolicy {
        Objects.requireNonNull(connectTimeout, "connectTimeout");
        Objects.requireNonNull(readTimeout, "readTimeout");
        Objects.requireNonNull(callDeadline, "callDeadline");
        requirePositive(connectTimeout, "connectTimeout");
        requirePositive(readTimeout, "readTimeout");
        requirePositive(callDeadline, "callDeadline");
        if (callDeadline.compareTo(connectTimeout) < 0) {
            throw new IllegalArgumentException(
                    "callDeadline must not be shorter than connectTimeout");
        }
        if (callDeadline.compareTo(readTimeout) < 0) {
            throw new IllegalArgumentException("callDeadline must not be shorter than readTimeout");
        }
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        if (maxAttempts > 5) {
            throw new IllegalArgumentException("maxAttempts must not exceed 5");
        }
    }

    /** Returns the compatibility policy used by pre-policy HTTP method overloads. */
    public static CrawlerRequestPolicy legacyDefault() {
        return LEGACY_DEFAULT;
    }

    /** Returns the bounded policy for scheduled news and sentiment refreshes. */
    public static CrawlerRequestPolicy backgroundNews() {
        return BACKGROUND_NEWS;
    }

    /** Returns the single-attempt policy for latency-sensitive request paths. */
    public static CrawlerRequestPolicy interactive() {
        return INTERACTIVE;
    }

    private static void requirePositive(Duration duration, String name) {
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
