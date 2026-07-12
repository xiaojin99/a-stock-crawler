package com.stock.crawler.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class CrawlerRequestPolicyTest {

    @Test
    void constructor_rejectsDeadlineShorterThanConnectTimeout() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new CrawlerRequestPolicy(
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(1),
                        1));

        assertEquals("callDeadline must not be shorter than connectTimeout", exception.getMessage());
    }

    @Test
    void backgroundNews_limitsTotalAttemptsToTwo() {
        assertEquals(2, CrawlerRequestPolicy.backgroundNews().maxAttempts());
    }

    @Test
    void interactive_hasSingleAttempt() {
        assertEquals(1, CrawlerRequestPolicy.interactive().maxAttempts());
    }

    @Test
    void constructor_rejectsExcessiveAttemptCount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CrawlerRequestPolicy(
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(2),
                        6));
    }
}
