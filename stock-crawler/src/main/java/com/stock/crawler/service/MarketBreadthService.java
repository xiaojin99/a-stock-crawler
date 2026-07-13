package com.stock.crawler.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.crawler.model.DataResult;
import com.stock.crawler.model.MarketBreadthSnapshot;
import com.stock.crawler.util.CrawlerRequestPolicy;
import com.stock.crawler.util.HttpUtils;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 从东方财富涨跌分布与全市场总数接口聚合 A 股市场广度。
 */
public class MarketBreadthService {

    private static final int SCHEMA_VERSION = 2;
    private static final int MIN_BUCKET = -11;
    private static final int MAX_BUCKET = 11;
    private static final int STRONG_BUCKET = 5;
    private static final int MIN_TOTAL_COUNT = 1000;
    private static final int MAX_TOTAL_COUNT = 10000;
    private static final ZoneId MARKET_ZONE = ZoneId.of("Asia/Shanghai");
    private static final String SOURCE = "market-breadth:eastmoney-distribution";
    private static final String DISTRIBUTION_URL =
            "https://push2ex.eastmoney.com/getTopicZDFenBu"
                    + "?ut=7eea3edcaed734bea9cbfc24409ed989&dpt=wz.ztzt";
    private static final String TOTAL_URL =
            "https://push2delay.eastmoney.com/api/qt/clist/get?pn=1&pz=1&po=1&np=1&fltt=2&invt=2"
                    + "&fid=f3&fs=m:0+t:6,m:0+t:80,m:1+t:2,m:1+t:23&fields=f2,f3";
    private static final Map<String, String> REQUEST_HEADERS =
            Map.of("Referer", "https://quote.eastmoney.com/center/gridlist.html");

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
            return DataResult.success(fetchAndAggregate(), SOURCE, elapsedMs(startedAt));
        } catch (IOException | RuntimeException ex) {
            return DataResult.failure(SOURCE, ex.getMessage(), elapsedMs(startedAt));
        }
    }

    private MarketBreadthSnapshot fetchAndAggregate() throws IOException {
        Map<Integer, Integer> buckets = fetchDistribution();
        long activeCount = buckets.values().stream().mapToLong(Integer::longValue).sum();
        if (activeCount <= 0) {
            throw new IOException("EastMoney market breadth distribution has no active securities");
        }

        int totalCount = fetchTotalCount();
        if (activeCount > totalCount) {
            throw new IOException(
                    "EastMoney market breadth active count " + activeCount
                            + " exceeds data.total " + totalCount);
        }

        int riseCount = sumBuckets(buckets, 1, MAX_BUCKET);
        int fallCount = sumBuckets(buckets, MIN_BUCKET, -1);
        int flatCount = buckets.getOrDefault(0, 0);
        return new MarketBreadthSnapshot(
                SCHEMA_VERSION,
                Instant.now(clock),
                riseCount,
                fallCount,
                flatCount,
                totalCount - Math.toIntExact(activeCount),
                buckets.getOrDefault(MAX_BUCKET, 0),
                buckets.getOrDefault(MIN_BUCKET, 0),
                sumBuckets(buckets, STRONG_BUCKET, MAX_BUCKET),
                sumBuckets(buckets, MIN_BUCKET, -STRONG_BUCKET),
                null);
    }

    private Map<Integer, Integer> fetchDistribution() throws IOException {
        String response = httpGetter.get(
                DISTRIBUTION_URL, REQUEST_HEADERS, CrawlerRequestPolicy.backgroundNews());
        JsonNode root = objectMapper.readTree(response);
        validateSuccessResponse(root, "distribution");
        JsonNode data = root.path("data");
        validateQuoteDate(data.get("qdate"));

        JsonNode distribution = data.get("fenbu");
        if (distribution == null || !distribution.isArray()) {
            throw new IOException(
                    "EastMoney market breadth distribution response does not contain data.fenbu");
        }

        Map<Integer, Integer> buckets = new HashMap<>();
        for (JsonNode item : distribution) {
            if (!item.isObject() || item.size() != 1) {
                throw new IOException("EastMoney market breadth bucket must contain exactly one value");
            }
            Iterator<Map.Entry<String, JsonNode>> fields = item.fields();
            Map.Entry<String, JsonNode> field = fields.next();
            int bucket = parseBucket(field.getKey());
            int count = parseBucketCount(bucket, field.getValue());
            if (buckets.putIfAbsent(bucket, count) != null) {
                throw new IOException("EastMoney market breadth contains duplicate bucket " + bucket);
            }
        }
        return buckets;
    }

    private int fetchTotalCount() throws IOException {
        String response = httpGetter.get(
                TOTAL_URL, REQUEST_HEADERS, CrawlerRequestPolicy.backgroundNews());
        JsonNode root = objectMapper.readTree(response);
        validateSuccessResponse(root, "total");
        JsonNode total = root.path("data").get("total");
        if (total == null || !total.isIntegralNumber() || !total.canConvertToInt()) {
            throw new IOException(
                    "EastMoney market breadth total response does not contain integer data.total");
        }
        int totalCount = total.intValue();
        if (totalCount < MIN_TOTAL_COUNT || totalCount > MAX_TOTAL_COUNT) {
            throw new IOException(
                    "EastMoney market breadth data.total is outside expected range: " + totalCount);
        }
        return totalCount;
    }

    private void validateSuccessResponse(JsonNode root, String responseName) throws IOException {
        if (root == null || !root.isObject()) {
            throw new IOException("EastMoney market breadth " + responseName + " response is not an object");
        }
        JsonNode responseCode = root.get("rc");
        if (responseCode == null || !responseCode.isIntegralNumber() || responseCode.intValue() != 0) {
            throw new IOException(
                    "EastMoney market breadth " + responseName + " response has invalid rc");
        }
        if (!root.path("data").isObject()) {
            throw new IOException(
                    "EastMoney market breadth " + responseName + " response does not contain data");
        }
    }

    private void validateQuoteDate(JsonNode quoteDate) throws IOException {
        if (quoteDate == null || !quoteDate.isIntegralNumber()) {
            throw new IOException(
                    "EastMoney market breadth distribution response does not contain integer qdate");
        }
        LocalDate actualDate;
        try {
            actualDate = LocalDate.parse(
                    Integer.toString(quoteDate.intValue()), DateTimeFormatter.BASIC_ISO_DATE);
        } catch (DateTimeParseException ex) {
            throw new IOException("EastMoney market breadth qdate is invalid", ex);
        }
        LocalDate expectedDate = Instant.now(clock).atZone(MARKET_ZONE).toLocalDate();
        if (!actualDate.equals(expectedDate)) {
            throw new IOException(
                    "EastMoney market breadth qdate " + actualDate
                            + " does not match current Shanghai date " + expectedDate);
        }
    }

    private int parseBucket(String value) throws IOException {
        int bucket;
        try {
            bucket = Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IOException("EastMoney market breadth bucket is not an integer: " + value, ex);
        }
        if (bucket < MIN_BUCKET || bucket > MAX_BUCKET) {
            throw new IOException("EastMoney market breadth bucket is outside -11..11: " + bucket);
        }
        return bucket;
    }

    private int parseBucketCount(int bucket, JsonNode value) throws IOException {
        if (value == null
                || !value.isIntegralNumber()
                || !value.canConvertToInt()
                || value.intValue() < 0) {
            throw new IOException(
                    "EastMoney market breadth bucket " + bucket
                            + " count must be a non-negative integer");
        }
        return value.intValue();
    }

    private int sumBuckets(Map<Integer, Integer> buckets, int start, int end) {
        int total = 0;
        for (int bucket = start; bucket <= end; bucket++) {
            total += buckets.getOrDefault(bucket, 0);
        }
        return total;
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    @FunctionalInterface
    interface HttpGetter {
        String get(String url, Map<String, String> headers, CrawlerRequestPolicy policy)
                throws IOException;
    }
}
