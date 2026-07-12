package com.stock.crawler.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.crawler.model.DataResult;
import com.stock.crawler.model.MarketBreadthSnapshot;
import com.stock.crawler.util.CrawlerRequestPolicy;
import com.stock.crawler.util.HttpUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;

/**
 * 从东方财富全市场行情列表聚合 A 股市场广度。
 */
public class MarketBreadthService {

    private static final int SCHEMA_VERSION = 1;
    /**
     * 涨跌停近似阈值，不等同于交易所按板块和 ST 状态计算的精确涨跌停。
     */
    private static final BigDecimal LIMIT_THRESHOLD = new BigDecimal("9.8");
    private static final BigDecimal STRONG_THRESHOLD = new BigDecimal("4.0");
    private static final String SOURCE = "market-breadth:eastmoney-clist";
    private static final String MARKET_BREADTH_URL =
            "https://push2.eastmoney.com/api/qt/clist/get?pn=1&pz=6000&po=1&np=1&fltt=2&invt=2"
                    + "&fid=f3&fs=m:0+t:6,m:0+t:80,m:1+t:2,m:1+t:23&fields=f2,f3";

    private final HttpGetter httpGetter;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public MarketBreadthService() {
        this(HttpUtils::getEastMoney, Clock.systemUTC());
    }

    MarketBreadthService(HttpGetter httpGetter, Clock clock) {
        this.httpGetter = httpGetter;
        this.clock = clock;
        this.objectMapper = new ObjectMapper();
    }

    public DataResult<MarketBreadthSnapshot> getMarketBreadth() {
        long startedAt = System.nanoTime();
        try {
            String response = httpGetter.get(
                    MARKET_BREADTH_URL,
                    Map.of("Referer", "https://quote.eastmoney.com/center/gridlist.html"),
                    CrawlerRequestPolicy.backgroundNews());
            return DataResult.success(parse(response), SOURCE, elapsedMs(startedAt));
        } catch (IOException | RuntimeException ex) {
            return DataResult.failure(SOURCE, ex.getMessage(), elapsedMs(startedAt));
        }
    }

    private MarketBreadthSnapshot parse(String response) throws IOException {
        JsonNode diff = objectMapper.readTree(response).path("data").path("diff");
        if (!diff.isArray()) {
            throw new IOException("EastMoney market breadth response does not contain data.diff");
        }
        int riseCount = 0;
        int fallCount = 0;
        int flatCount = 0;
        int suspendedCount = 0;
        int limitUpCount = 0;
        int limitDownCount = 0;
        int strongUpCount = 0;
        int strongDownCount = 0;
        BigDecimal changeTotal = BigDecimal.ZERO;
        int activeCount = 0;

        for (JsonNode item : diff) {
            JsonNode priceNode = item.get("f2");
            JsonNode changeNode = item.get("f3");
            if (priceNode == null || changeNode == null) {
                continue;
            }
            if (!priceNode.isNumber() || !changeNode.isNumber()) {
                if (isSuspendedValue(priceNode) || isSuspendedValue(changeNode)) {
                    suspendedCount++;
                }
                continue;
            }
            BigDecimal price = priceNode.decimalValue();
            BigDecimal changePercent = changeNode.decimalValue();
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                suspendedCount++;
                continue;
            }

            activeCount++;
            changeTotal = changeTotal.add(changePercent);
            int direction = changePercent.compareTo(BigDecimal.ZERO);
            if (direction > 0) {
                riseCount++;
            } else if (direction < 0) {
                fallCount++;
            } else {
                flatCount++;
            }
            if (changePercent.compareTo(LIMIT_THRESHOLD) >= 0) {
                limitUpCount++;
            }
            if (changePercent.compareTo(LIMIT_THRESHOLD.negate()) <= 0) {
                limitDownCount++;
            }
            if (changePercent.compareTo(STRONG_THRESHOLD) >= 0) {
                strongUpCount++;
            }
            if (changePercent.compareTo(STRONG_THRESHOLD.negate()) <= 0) {
                strongDownCount++;
            }
        }
        if (activeCount == 0) {
            throw new IOException("EastMoney market breadth response has no active securities");
        }
        return new MarketBreadthSnapshot(
                SCHEMA_VERSION,
                Instant.now(clock),
                riseCount,
                fallCount,
                flatCount,
                suspendedCount,
                limitUpCount,
                limitDownCount,
                strongUpCount,
                strongDownCount,
                changeTotal.divide(BigDecimal.valueOf(activeCount), 4, RoundingMode.HALF_UP));
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private boolean isSuspendedValue(JsonNode value) {
        return value != null && "-".equals(value.asText());
    }

    @FunctionalInterface
    interface HttpGetter {
        String get(String url, Map<String, String> headers, CrawlerRequestPolicy policy)
                throws IOException;
    }
}
