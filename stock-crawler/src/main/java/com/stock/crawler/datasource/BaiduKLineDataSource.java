package com.stock.crawler.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.crawler.model.KLineData;
import com.stock.crawler.model.StockQuote;
import com.stock.crawler.util.ParseUtils;
import com.stock.crawler.util.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 百度股市通 K 线数据源。
 *
 * <p>百度接口封 IP 风险较低，日 K 返回字段自带 MA5/MA10/MA20。当前 KLineData
 * 还没有均线字段，这里先作为低风险 K 线源接入。</p>
 */
public class BaiduKLineDataSource implements MarketDataSource {

    private static final Logger log = LoggerFactory.getLogger(BaiduKLineDataSource.class);

    private static final String BAIDU_KLINE_URL =
            "https://finance.pae.baidu.com/selfselect/getstockquotation"
                    + "?all=1&isIndex=%s&isBk=false&isBlock=false&isFutures=false"
                    + "&isStock=%s&newFormat=1&group=quotation_kline_ab&finClientType=pc"
                    + "&code=%s&ktype=1";
    private static final DateTimeFormatter KLINE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "BaiduKLine";
    }

    @Override
    public List<StockQuote> getRealTimeQuotes(List<String> stockCodes) {
        return new ArrayList<>();
    }

    @Override
    public List<KLineData> getKLineData(String stockCode, String period, int days) {
        if (!supportsPeriod(period)) {
            return new ArrayList<>();
        }
        if (isIndexCode(stockCode)) {
            return new ArrayList<>();
        }
        try {
            String code = stripCodePrefix(stockCode);
            String url = String.format(BAIDU_KLINE_URL, false, true, code);
            String body = getWithJdkHttpClient(url);
            return parseKLineData(stockCode, normalizePeriod(period), days, body);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("baidu_kline_interrupted stockCode={} period={} days={}",
                    stockCode, period, days);
            return new ArrayList<>();
        } catch (IOException ex) {
            log.warn("baidu_kline_io_failed stockCode={} period={} days={} message={}",
                    stockCode, period, days, ex.getMessage());
            log.debug("baidu_kline_io_failed_stack stockCode={} period={} days={}",
                    stockCode, period, days, ex);
            return new ArrayList<>();
        } catch (RuntimeException ex) {
            log.warn("baidu_kline_runtime_failed stockCode={} period={} days={} message={}",
                    stockCode, period, days, ex.getMessage());
            log.debug("baidu_kline_runtime_failed_stack stockCode={} period={} days={}",
                    stockCode, period, days, ex);
            return new ArrayList<>();
        }
    }

    private String getWithJdkHttpClient(String url) throws IOException, InterruptedException {
        RateLimiter.throttle(url);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "Mozilla/5.0")
                .header("Accept", "application/vnd.finance-web.v1+json")
                .header("Origin", "https://gushitong.baidu.com")
                .header("Referer", "https://gushitong.baidu.com/")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode() + " from: " + url);
        }
        return response.body();
    }

    private boolean supportsPeriod(String period) {
        String normalized = normalizePeriod(period);
        return "day".equals(normalized);
    }

    private String normalizePeriod(String period) {
        if (period == null || period.isBlank()) {
            return "day";
        }
        return switch (period.trim().toLowerCase()) {
            case "day", "daily", "d" -> "day";
            default -> period.trim().toLowerCase();
        };
    }

    private List<KLineData> parseKLineData(String stockCode, String period, int days, String body)
            throws IOException {
        JsonNode root = objectMapper.readTree(body);
        if (!"0".equals(root.path("ResultCode").asText())) {
            log.warn("baidu_kline_response_failed stockCode={} resultCode={} message={}",
                    stockCode, root.path("ResultCode").asText(), root.path("ResultMessage").asText());
            return new ArrayList<>();
        }

        JsonNode marketDataNode = root.path("Result").path("newMarketData").path("marketData");
        if (!marketDataNode.isTextual() || marketDataNode.asText().isBlank()) {
            return new ArrayList<>();
        }

        String[] rows = marketDataNode.asText().split(";");
        int start = Math.max(0, rows.length - Math.max(days, 0));
        List<KLineData> klineList = new ArrayList<>();
        for (int i = start; i < rows.length; i++) {
            String row = rows[i];
            if (row == null || row.isBlank()) {
                continue;
            }
            String[] fields = row.split(",");
            if (fields.length < 11) {
                continue;
            }
            klineList.add(KLineData.builder()
                    .code(stockCode)
                    .date(LocalDate.parse(fields[1], KLINE_DATE))
                    .open(ParseUtils.parseBigDecimal(fields[2]))
                    .close(ParseUtils.parseBigDecimal(fields[3]))
                    .volume(ParseUtils.parseLong(fields[4]))
                    .high(ParseUtils.parseBigDecimal(fields[5]))
                    .low(ParseUtils.parseBigDecimal(fields[6]))
                    .amount(ParseUtils.parseBigDecimal(fields[7]))
                    .changePercent(ParseUtils.parseBigDecimal(fields[9]))
                    .turnoverRate(ParseUtils.parseBigDecimal(fields[10]))
                    .period(period)
                    .build());
        }
        return klineList;
    }

    private String stripCodePrefix(String stockCode) {
        if (stockCode == null) {
            return "";
        }
        String code = stockCode.trim().toLowerCase();
        if (code.matches("^(sh|sz|bj)\\d{6}$")) {
            return code.substring(2);
        }
        if (code.matches("^\\d{6}\\.(sh|sz|bj)$")) {
            return code.substring(0, 6);
        }
        return code;
    }

    private boolean isIndexCode(String stockCode) {
        if (stockCode == null) {
            return false;
        }
        String code = stockCode.trim().toLowerCase();
        return code.matches("^sh000\\d{3}$")
                || code.matches("^sz399\\d{3}$")
                || code.matches("^(000\\d{3}|399\\d{3})\\.(sh|sz)$");
    }

    @Override
    public List<StockQuote> searchStock(String keyword) {
        return new ArrayList<>();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public int getPriority() {
        return 2;
    }
}
