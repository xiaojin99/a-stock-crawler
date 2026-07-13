package com.stock.crawler.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.crawler.model.DataResult;
import com.stock.crawler.model.MarketBreadthSnapshot;
import com.stock.crawler.util.CrawlerRequestPolicy;
import com.stock.crawler.util.HttpUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("integration")
class MarketBreadthServiceIntegrationTest {

    private static final String POOL_URL =
            "https://push2ex.eastmoney.com/getTopic%sPool"
                    + "?ut=7eea3edcaed734bea9cbfc24409ed989&dpt=wz.ztzt"
                    + "&Pageindex=0&pagesize=1&date=%s";

    @Test
    void liveDistributionRemainsConsistentWithLimitPools() throws Exception {
        DataResult<MarketBreadthSnapshot> result = new MarketBreadthService().getMarketBreadth();

        assertTrue(result.isSuccess(), result.getMessage());
        assertTrue(result.getElapsedMs() < 10_000, "refresh took " + result.getElapsedMs() + "ms");
        MarketBreadthSnapshot snapshot = result.getData();
        int total = snapshot.getActiveCount() + snapshot.getSuspendedCount();
        assertTrue(total >= 1000 && total <= 10000, "unexpected total: " + total);

        String tradeDate = LocalDate.now(ZoneId.of("Asia/Shanghai"))
                .format(DateTimeFormatter.BASIC_ISO_DATE);
        assertEquals(poolTotal("ZT", tradeDate), snapshot.getLimitUpCount());
        assertEquals(poolTotal("DT", tradeDate), snapshot.getLimitDownCount());
    }

    private int poolTotal(String pool, String tradeDate) throws Exception {
        String response = HttpUtils.getEastMoney(
                POOL_URL.formatted(pool, tradeDate),
                Map.of("Referer", "https://quote.eastmoney.com/"),
                CrawlerRequestPolicy.backgroundNews());
        JsonNode total = new ObjectMapper().readTree(response).path("data").path("tc");
        assumeTrue(
                total.isIntegralNumber(),
                "EastMoney limit pool is temporarily unavailable; response has no data.tc");
        return total.intValue();
    }
}
