package com.stock.crawler.service;

import com.stock.crawler.model.DataResult;
import com.stock.crawler.model.MarketBreadthSnapshot;
import com.stock.crawler.util.CrawlerRequestPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketBreadthServiceTest {

    private static final Clock TRADING_DAY_CLOCK =
            Clock.fixed(Instant.parse("2026-07-13T02:40:00Z"), ZoneOffset.UTC);

    @Test
    void fetchesOnlyDistributionAndTotalThenBuildsVersionTwoSnapshot() {
        AtomicInteger requestCount = new AtomicInteger();
        MarketBreadthService service = new MarketBreadthService(
                (url, headers, policy) -> {
                    requestCount.incrementAndGet();
                    assertEquals(CrawlerRequestPolicy.backgroundNews(), policy);
                    if (url.contains("getTopicZDFenBu")) {
                        return validDistribution();
                    }
                    if (url.contains("pz=1")) {
                        return """
                            {"rc":0,"data":{"total":1040,"diff":[]}}
                            """;
                    }
                    throw new AssertionError("unexpected URL: " + url);
                },
                TRADING_DAY_CLOCK);

        DataResult<MarketBreadthSnapshot> result = service.getMarketBreadth();

        assertTrue(result.isSuccess());
        assertEquals(2, requestCount.get());
        assertEquals("market-breadth:eastmoney-distribution", result.getSource());
        assertEquals(2, result.getData().getSchemaVersion());
        assertEquals(21, result.getData().getRiseCount());
        assertEquals(10, result.getData().getFallCount());
        assertEquals(4, result.getData().getFlatCount());
        assertEquals(1005, result.getData().getSuspendedCount());
        assertEquals(8, result.getData().getLimitUpCount());
        assertEquals(2, result.getData().getLimitDownCount());
        assertEquals(15, result.getData().getStrongUpCount());
        assertEquals(5, result.getData().getStrongDownCount());
        assertNull(result.getData().getAverageChangePercent());
    }

    @ParameterizedTest(name = "rejects invalid distribution: {0}")
    @MethodSource("invalidDistributions")
    void rejectsInvalidDistributionResponses(String scenario, String distribution, String messageFragment) {
        MarketBreadthService service = serviceReturning(distribution, 5537);

        DataResult<MarketBreadthSnapshot> result = service.getMarketBreadth();

        assertFalse(result.isSuccess());
        assertNull(result.getData());
        assertTrue(result.getMessage().contains(messageFragment), result.getMessage());
    }

    @Test
    void rejectsTotalOutsideExpectedRange() {
        MarketBreadthService service = serviceReturning(validDistribution(), 999);

        DataResult<MarketBreadthSnapshot> result = service.getMarketBreadth();

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("data.total"), result.getMessage());
    }

    @Test
    void rejectsActiveCountGreaterThanTotalCount() {
        MarketBreadthService service = new MarketBreadthService(
                (url, headers, policy) -> url.contains("getTopicZDFenBu")
                        ? """
                            {"rc":0,"data":{"qdate":20260713,"fenbu":[{"1":1001}]}}
                            """
                        : """
                            {"rc":0,"data":{"total":1000}}
                            """,
                TRADING_DAY_CLOCK);

        DataResult<MarketBreadthSnapshot> result = service.getMarketBreadth();

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("active count"), result.getMessage());
    }

    private MarketBreadthService serviceReturning(String distribution, int total) {
        return new MarketBreadthService(
                (url, headers, policy) -> url.contains("getTopicZDFenBu")
                        ? distribution
                        : "{\"rc\":0,\"data\":{\"total\":" + total + "}}",
                TRADING_DAY_CLOCK);
    }

    private static Stream<Arguments> invalidDistributions() {
        return Stream.of(
                Arguments.of(
                        "missing fenbu",
                        "{\"rc\":0,\"data\":{\"qdate\":20260713}}",
                        "data.fenbu"),
                Arguments.of(
                        "illegal bucket",
                        "{\"rc\":0,\"data\":{\"qdate\":20260713,\"fenbu\":[{\"12\":1}]}}",
                        "bucket"),
                Arguments.of(
                        "negative count",
                        "{\"rc\":0,\"data\":{\"qdate\":20260713,\"fenbu\":[{\"1\":-1}]}}",
                        "non-negative integer"),
                Arguments.of(
                        "duplicate bucket",
                        "{\"rc\":0,\"data\":{\"qdate\":20260713,\"fenbu\":[{\"1\":1},{\"1\":2}]}}",
                        "duplicate"),
                Arguments.of(
                        "expired date",
                        "{\"rc\":0,\"data\":{\"qdate\":20260712,\"fenbu\":[{\"1\":1}]}}",
                        "qdate"),
                Arguments.of(
                        "non-integer count",
                        "{\"rc\":0,\"data\":{\"qdate\":20260713,\"fenbu\":[{\"1\":1.5}]}}",
                        "non-negative integer"));
    }

    private static String validDistribution() {
        return """
            {"rc":0,"data":{"qdate":20260713,"fenbu":[
              {"-11":2},{"-5":3},{"-1":5},{"0":4},
              {"1":6},{"5":7},{"11":8}
            ]}}
            """;
    }
}
