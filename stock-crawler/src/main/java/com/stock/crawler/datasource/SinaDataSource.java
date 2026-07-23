package com.stock.crawler.datasource;

import com.stock.crawler.model.KLineData;
import com.stock.crawler.model.StockQuote;
import com.stock.crawler.util.CrawlerRequestPolicy;
import com.stock.crawler.util.HttpUtils;
import com.stock.crawler.util.ParseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.crawler.exception.MarketDataAccessException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 新浪财经数据源
 * 提供实时行情与股票搜索
 */
public class SinaDataSource implements MarketDataSource {

    private static final Logger log = LoggerFactory.getLogger(SinaDataSource.class);

    private static final String SINA_QUOTE_URL = "http://hq.sinajs.cn/rn=%d&list=%s";
    private static final String SINA_SUGGEST_URL =
            "https://suggest3.sinajs.cn/suggest/type=11,12,13,14,15&key=%s&name=suggestdata_";
    // 新浪 K 线 API: scale=240(日K), 1200(周K), 5200(月K)
    private static final String SINA_KLINE_URL =
            "http://quotes.sina.cn/cn/api/jsonp_v2.php/=/CN_MarketDataService.getKLineData?symbol=%s&scale=%d&ma=no&datalen=%d";
    private static final DateTimeFormatter KLINE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter QUOTE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final HttpBodyFetcher httpBodyFetcher;
    private final ObjectMapper objectMapper;

    public SinaDataSource() {
        // 新浪行情与搜索接口返回 GBK 编码，必须按 GBK 解码，否则中文名称乱码
        this((url, headers, policy) -> HttpUtils.getWithCharset(url, headers, HttpUtils.GBK, policy));
    }

    SinaDataSource(HttpBodyFetcher httpBodyFetcher) {
        this.httpBodyFetcher = Objects.requireNonNull(httpBodyFetcher, "httpBodyFetcher");
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getName() {
        return "Sina";
    }

    @Override
    public List<StockQuote> getRealTimeQuotes(List<String> stockCodes) {
        if (stockCodes == null || stockCodes.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            String codesParam = String.join(",", stockCodes);
            String url = String.format(SINA_QUOTE_URL, System.currentTimeMillis(), codesParam);
            String body = httpBodyFetcher.get(
                    url,
                    Map.of("Referer", "http://finance.sina.com.cn"),
                    CrawlerRequestPolicy.interactive());
            return parseSinaQuotes(body);
        } catch (IOException ex) {
            log.warn("sina_quote_io_failed message={}", ex.getMessage(), ex);
            return new ArrayList<>();
        } catch (RuntimeException ex) {
            log.warn("sina_quote_runtime_failed message={}", ex.getMessage(), ex);
            return new ArrayList<>();
        }
    }

    /**
     * 解析新浪行情数据
     * 格式: var hq_str_sh600519="贵州茅台,1800.00,..."
     */
    private List<StockQuote> parseSinaQuotes(String body) {
        List<StockQuote> quotes = new ArrayList<>();
        String[] lines = body.split(";");

        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            try {
                int eqIndex = line.indexOf("=\"");
                if (eqIndex == -1) {
                    continue;
                }
                String code = line.substring(line.indexOf("_str_") + 5, eqIndex);
                String data = line.substring(eqIndex + 2, line.length() - 1);

                if (data.isEmpty()) {
                    continue;
                }
                String[] fields = data.split(",");
                if (fields.length < 32) {
                    continue;
                }

                BigDecimal price = ParseUtils.parseRequiredBigDecimal(fields[3]);
                BigDecimal preClose = ParseUtils.parseRequiredBigDecimal(fields[2]);
                BigDecimal open = ParseUtils.parseRequiredBigDecimal(fields[1]);
                BigDecimal high = ParseUtils.parseRequiredBigDecimal(fields[4]);
                BigDecimal low = ParseUtils.parseRequiredBigDecimal(fields[5]);
                if (isUnavailableQuoteSnapshot(price, preClose)) {
                    log.debug("sina_quote_unavailable_snapshot code={} price={} preClose={}",
                            code, price, preClose);
                    continue;
                }
                validateQuotePrices(price, preClose, open, high, low);

                StockQuote quote = StockQuote.builder()
                        .code(code)
                        .name(fields[0])
                        .open(open)
                        .preClose(preClose)
                        .price(price)
                        .high(high)
                        .low(low)
                        .volume(ParseUtils.parseRequiredLong(fields[8]))
                        .amount(ParseUtils.parseRequiredBigDecimal(fields[9]))
                        .bid1Price(ParseUtils.parseNullableBigDecimal(fields[10]))
                        .bid1Volume(ParseUtils.parseNullableLong(fields[11]))
                        .ask1Price(ParseUtils.parseNullableBigDecimal(fields[20]))
                        .ask1Volume(ParseUtils.parseNullableLong(fields[21]))
                        .source("Sina")
                        .time(LocalDateTime.parse(fields[30] + " " + fields[31], QUOTE_TIME))
                        .build();

                // 计算涨跌
                if (quote.getPreClose() != null && quote.getPreClose().compareTo(BigDecimal.ZERO) > 0) {
                    quote.setChange(quote.getPrice().subtract(quote.getPreClose()));
                    quote.setChangePercent(quote.getChange()
                            .multiply(new BigDecimal("100"))
                            .divide(quote.getPreClose(), 2, RoundingMode.HALF_UP));
                }
                quotes.add(quote);
            } catch (RuntimeException ex) {
                log.warn("sina_quote_parse_failed line={} message={}", line, ex.getMessage(), ex);
            }
        }
        return quotes;
    }

    private void validateQuotePrices(
            BigDecimal price,
            BigDecimal preClose,
            BigDecimal open,
            BigDecimal high,
            BigDecimal low) {
        if (price.signum() < 0 || preClose.signum() < 0
                || open.signum() < 0 || high.signum() < 0 || low.signum() < 0) {
            throw new IllegalArgumentException("Sina quote contains invalid price values");
        }
        if (high.signum() > 0 && low.signum() > 0 && high.compareTo(low) < 0) {
            throw new IllegalArgumentException("Sina quote high price is below low price");
        }
    }

    private boolean isUnavailableQuoteSnapshot(BigDecimal price, BigDecimal preClose) {
        return price.signum() == 0 || preClose.signum() == 0;
    }

    @Override
    public List<KLineData> getKLineData(String stockCode, String period, int days) {
        try {
            int scale = getSinaScale(period);
            String url = String.format(SINA_KLINE_URL, stockCode, scale, days);
            String body = httpBodyFetcher.get(
                    url,
                    Map.of("Referer", "http://finance.sina.com.cn"),
                    CrawlerRequestPolicy.interactive());
            return parseSinaKLineData(stockCode, period, body);
        } catch (Exception ex) {
            throw new MarketDataAccessException(
                    getName(), "kline", "Sina K-line request or parsing failed", ex);
        }
    }

    @Override
    public boolean supportsKLinePeriod(String period) {
        if (period == null || period.isBlank()) {
            return true;
        }
        return switch (period.trim().toLowerCase()) {
            case "day", "daily", "d", "week", "weekly", "w", "month", "monthly", "m" -> true;
            default -> false;
        };
    }

    /**
     * 新浪 K 线周期映射: scale 参数
     */
    private int getSinaScale(String period) {
        if (period == null || period.isBlank()) {
            return 240;
        }
        return switch (period.toLowerCase()) {
            case "day", "daily" -> 240;
            case "week", "weekly" -> 1200;
            case "month", "monthly" -> 5200;
            default -> 240;
        };
    }

    /**
     * 解析新浪 K 线数据
     * JSONP 格式: =([{...},{...}]);
     * 每个 JSON 对象含: day, open, high, low, close, volume
     */
    private List<KLineData> parseSinaKLineData(String stockCode, String period, String body)
            throws IOException {
        List<KLineData> klineList = new ArrayList<>();
        // 移除 JSONP 包装: =([...]);  -> [...]
        int start = body.indexOf("([");
        int end = body.indexOf("])");
        if (start == -1 || end == -1) {
            return klineList;
        }
        String json = body.substring(start + 1, end + 1);

        JsonNode array = objectMapper.readTree(json);
        if (!array.isArray()) {
            return klineList;
        }

        for (JsonNode node : array) {
            KLineData kline = KLineData.builder()
                    .code(stockCode)
                    .date(LocalDate.parse(node.path("day").asText(), KLINE_DATE))
                    .open(ParseUtils.parseBigDecimal(textValue(node, "open", "0")))
                    .high(ParseUtils.parseBigDecimal(textValue(node, "high", "0")))
                    .low(ParseUtils.parseBigDecimal(textValue(node, "low", "0")))
                    .close(ParseUtils.parseBigDecimal(textValue(node, "close", "0")))
                    .volume(node.path("volume").asLong(0))
                    .period(period)
                    .build();
            klineList.add(kline);
        }
        return klineList;
    }

    private String textValue(JsonNode parent, String fieldName, String defaultValue) {
        JsonNode value = parent == null ? null : parent.get(fieldName);
        return value == null || value.isNull() ? defaultValue : value.asText();
    }

    @Override
    public List<StockQuote> searchStock(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return new ArrayList<>();
        }
        try {
            String encodedKeyword = URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8);
            String url = String.format(SINA_SUGGEST_URL, encodedKeyword);
            String body = httpBodyFetcher.get(
                    url,
                    Map.of(
                            "Referer", "https://finance.sina.com.cn",
                            "User-Agent", "Mozilla/5.0"),
                    CrawlerRequestPolicy.interactive());
            return parseSinaSuggestResult(body);
        } catch (IOException ex) {
            log.warn("sina_search_io_failed keyword={} message={}", keyword, ex.getMessage(), ex);
            return new ArrayList<>();
        } catch (RuntimeException ex) {
            log.warn("sina_search_runtime_failed keyword={} message={}", keyword, ex.getMessage(), ex);
            return new ArrayList<>();
        }
    }

    private List<StockQuote> parseSinaSuggestResult(String body) {
        if (body == null || body.isBlank()) {
            return new ArrayList<>();
        }
        int start = body.indexOf("=\"");
        int end = body.lastIndexOf("\"");
        if (start < 0 || end <= start) {
            return new ArrayList<>();
        }
        String payload = body.substring(start + 2, end);
        if (payload.isBlank()) {
            return new ArrayList<>();
        }
        List<StockQuote> results = new ArrayList<>();
        String[] items = payload.split(";");
        for (String item : items) {
            String[] fields = item.split(",");
            if (fields.length < 2) {
                continue;
            }
            String code = extractSuggestCode(fields);
            String name = extractSuggestName(fields, code);
            if (code.isBlank()) {
                continue;
            }
            results.add(StockQuote.builder()
                    .code(code)
                    .name(name)
                    .source("SinaSuggest")
                    .build());
            if (results.size() >= 20) {
                break;
            }
        }
        return results;
    }

    private String extractSuggestCode(String[] fields) {
        for (String field : fields) {
            String value = field == null ? "" : field.trim().toLowerCase();
            if (value.matches("^(sh|sz|bj)\\d{6}$") || value.matches("^hk\\d+$")) {
                return value;
            }
            if (value.matches("^\\d{6}\\.(sh|sz)$")) {
                String[] parts = value.split("\\.");
                if (parts.length == 2) {
                    return parts[1] + parts[0];
                }
            }
        }
        if (fields.length > 3) {
            return fields[3] == null ? "" : fields[3].trim().toLowerCase();
        }
        return "";
    }

    private String extractSuggestName(String[] fields, String code) {
        String normalizedCode = code == null ? "" : code.trim().toLowerCase();
        String digits = normalizedCode.replaceFirst("^(sh|sz|bj|hk)", "");
        for (String field : fields) {
            String value = field == null ? "" : field.trim();
            if (value.isBlank()) {
                continue;
            }
            String lower = value.toLowerCase();
            if (lower.equals(normalizedCode) || lower.equals(digits)) {
                continue;
            }
            if (lower.matches("^(sh|sz|bj)\\d{6}$") || lower.matches("^hk\\d+$")
                    || lower.matches("^\\d{6}\\.(sh|sz)$")) {
                continue;
            }
            if (value.matches("^\\d{5,}$")) {
                continue;
            }
            return value;
        }
        return "";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public int getPriority() {
        return 3;
    }

    @FunctionalInterface
    interface HttpBodyFetcher {
        String get(
                String url,
                Map<String, String> headers,
                CrawlerRequestPolicy policy) throws IOException;
    }
}
