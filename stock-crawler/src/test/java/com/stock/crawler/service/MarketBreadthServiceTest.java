package com.stock.crawler.service;

import com.stock.crawler.model.DataResult;
import com.stock.crawler.model.MarketBreadthSnapshot;
import com.stock.crawler.util.CrawlerRequestPolicy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketBreadthServiceTest {

    @Test
    void aggregatesEastMoneyQuoteDistributionIntoBreadthSnapshot() {
        MarketBreadthService service = new MarketBreadthService(
                (url, headers, policy) -> {
                    assertEquals(CrawlerRequestPolicy.backgroundNews(), policy);
                    return """
                        {"data":{"diff":[
                          {"f2":10.1,"f3":1.0},
                          {"f2":11.0,"f3":10.0},
                          {"f2":9.0,"f3":-10.0},
                          {"f2":10.0,"f3":0.0},
                          {"f2":"-","f3":"-"}
                        ]}}
                        """;
                },
                Clock.fixed(Instant.parse("2026-07-10T02:40:00Z"), ZoneOffset.UTC));

        DataResult<MarketBreadthSnapshot> result = service.getMarketBreadth();

        assertTrue(result.isSuccess());
        assertEquals("market-breadth:eastmoney-clist", result.getSource());
        assertEquals(2, result.getData().getRiseCount());
        assertEquals(1, result.getData().getFallCount());
        assertEquals(1, result.getData().getFlatCount());
        assertEquals(1, result.getData().getSuspendedCount());
        assertEquals(1, result.getData().getLimitUpCount());
        assertEquals(1, result.getData().getLimitDownCount());
        assertEquals(0, new BigDecimal("0.25").compareTo(result.getData().getAverageChangePercent()));
    }
}
