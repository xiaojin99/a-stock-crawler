package com.stock.crawler.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.crawler.model.KLineData;
import com.stock.crawler.model.StockQuote;
import com.stock.crawler.util.HttpUtils;
import com.stock.crawler.util.ParseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 东方财富数据源
 * 提供K线历史数据（实时行情由新浪提供）
 */
public class EastMoneyDataSource implements MarketDataSource {

    private static final Logger log = LoggerFactory.getLogger(EastMoneyDataSource.class);

    // 东方财富实时行情接口: f162=PE×100, f167=PB×100, f116=总市值, f168=换手率×100
    private static final String EASTMONEY_QUOTE_URL =
            "https://push2.eastmoney.com/api/qt/stock/get?secid=%s&fields=f116,f162,f167,f168";

    // 东方财富 K 线接口
    private static final String EASTMONEY_KLINE_URL =
            "https://push2his.eastmoney.com/api/qt/stock/kline/get" +
            "?secid=%s&fields1=f1,f2,f3,f4,f5,f6&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61" +
            "&klt=%s&fqt=1&end=20500101&lmt=%d";

    // 周期映射
    private static final int PERIOD_DAY = 101;
    private static final int PERIOD_WEEK = 102;
    private static final int PERIOD_MONTH = 103;

    private final ObjectMapper objectMapper;

    public EastMoneyDataSource() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getName() {
        return "EastMoney";
    }

    @Override
    public List<StockQuote> getRealTimeQuotes(List<String> stockCodes) {
        // 东方财富主要用于 K 线数据，实时行情由新浪提供
        return new ArrayList<>();
    }

    @Override
    public List<KLineData> getKLineData(String stockCode, String period, int days) {
        try {
            String secId = convertToSecId(stockCode);
            int periodCode = getPeriodCode(period);

            String url = String.format(EASTMONEY_KLINE_URL, secId, periodCode, days);
            String body = HttpUtils.getEastMoney(url, Map.of("Referer", "https://quote.eastmoney.com/"));
            return parseKLineData(stockCode, period, body);
        } catch (IOException ex) {
            log.warn("eastmoney_kline_io_failed stockCode={} period={} days={} message={}",
                    stockCode, period, days, ex.getMessage());
            log.debug("eastmoney_kline_io_failed_stack stockCode={} period={} days={}",
                    stockCode, period, days, ex);
            return new ArrayList<>();
        } catch (RuntimeException ex) {
            log.warn("eastmoney_kline_runtime_failed stockCode={} period={} days={} message={}",
                    stockCode, period, days, ex.getMessage());
            log.debug("eastmoney_kline_runtime_failed_stack stockCode={} period={} days={}",
                    stockCode, period, days, ex);
            return new ArrayList<>();
        }
    }

    private List<KLineData> parseKLineData(String stockCode, String period, String body) {
        List<KLineData> klineList = new ArrayList<>();
        try {
            body = unwrapJsonBody(body);

            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.path("data");

            if (data.isMissingNode() || data.isNull()) {
                return klineList;
            }

            JsonNode klines = data.path("klines");
            if (!klines.isArray()) {
                return klineList;
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            for (JsonNode klineNode : klines) {
                String klineStr = klineNode.asText();
                String[] fields = klineStr.split(",");

                if (fields.length < 6) {
                    continue;
                }

                KLineData kline = KLineData.builder()
                        .code(stockCode)
                        .date(LocalDate.parse(fields[0], formatter))
                        .open(ParseUtils.parseBigDecimal(fields[1]))
                        .close(ParseUtils.parseBigDecimal(fields[2]))
                        .high(ParseUtils.parseBigDecimal(fields[3]))
                        .low(ParseUtils.parseBigDecimal(fields[4]))
                        .volume(ParseUtils.parseLong(fields[5]))
                        .amount(fields.length > 6 ? ParseUtils.parseBigDecimal(fields[6]) : BigDecimal.ZERO)
                        .changePercent(fields.length > 7 ? ParseUtils.parseBigDecimal(fields[7]) : BigDecimal.ZERO)
                        .turnoverRate(fields.length > 8 ? ParseUtils.parseBigDecimal(fields[8]) : BigDecimal.ZERO)
                        .period(period)
                        .build();

                klineList.add(kline);
            }
        } catch (IOException ex) {
            log.warn("eastmoney_kline_parse_io_failed stockCode={} period={} message={}",
                    stockCode, period, ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            log.warn("eastmoney_kline_parse_runtime_failed stockCode={} period={} message={}",
                    stockCode, period, ex.getMessage(), ex);
        }
        return klineList;
    }

    private String unwrapJsonBody(String body) {
        if (body == null) {
            return "";
        }
        String trimmed = body.trim();
        int objectStart = trimmed.indexOf('{');
        int objectEnd = trimmed.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            return trimmed.substring(objectStart, objectEnd + 1);
        }
        return trimmed;
    }

    /**
     * 转换股票代码为东方财富格式
     * sh600519 -> 1.600519 / sz000001 -> 0.000001
     */
    private String convertToSecId(String stockCode) {
        if (stockCode.startsWith("sh")) {
            return "1." + stockCode.substring(2);
        } else if (stockCode.startsWith("sz")) {
            return "0." + stockCode.substring(2);
        } else if (stockCode.startsWith("hk")) {
            return "116." + stockCode.substring(2);
        }
        if (stockCode.startsWith("6")) {
            return "1." + stockCode;
        } else {
            return "0." + stockCode;
        }
    }

    private int getPeriodCode(String period) {
        if (period == null || period.isBlank()) {
            return PERIOD_DAY;
        }
        return switch (period.toLowerCase()) {
            case "day", "daily" -> PERIOD_DAY;
            case "week", "weekly" -> PERIOD_WEEK;
            case "month", "monthly" -> PERIOD_MONTH;
            default -> PERIOD_DAY;
        };
    }

    /**
     * 从东方财富获取估值指标（PE/PB/总市值），回填到已有的 StockQuote 中
     */
    public void enrichValuation(StockQuote quote) {
        if (quote == null || quote.getCode() == null) {
            return;
        }
        try {
            String secId = convertToSecId(quote.getCode());
            String url = String.format(EASTMONEY_QUOTE_URL, secId);
            String body = HttpUtils.getEastMoney(url, Map.of("Referer", "https://quote.eastmoney.com/"));
            JsonNode root = objectMapper.readTree(unwrapJsonBody(body));
            JsonNode data = root.path("data");
            if (data.isMissingNode() || data.isNull()) {
                return;
            }
            // f162=PE×100, f167=PB×100, f116=总市值, f168=换手率×100
            JsonNode peNode = data.get("f162");
            JsonNode pbNode = data.get("f167");
            JsonNode marketCapNode = data.get("f116");
            JsonNode turnoverNode = data.get("f168");
            if (peNode != null && !peNode.isNull()) {
                quote.setPe(peNode.decimalValue().divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            }
            if (pbNode != null && !pbNode.isNull()) {
                quote.setPb(pbNode.decimalValue().divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            }
            if (marketCapNode != null && !marketCapNode.isNull()) {
                quote.setMarketCap(marketCapNode.decimalValue());
            }
            if (turnoverNode != null && !turnoverNode.isNull()) {
                quote.setTurnoverRate(turnoverNode.decimalValue().divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            }
            log.debug("eastmoney_valuation_enriched code={} pe={} pb={} marketCap={} turnoverRate={}",
                    quote.getCode(), quote.getPe(), quote.getPb(), quote.getMarketCap(), quote.getTurnoverRate());
        } catch (Exception ex) {
            log.warn("eastmoney_valuation_failed code={} message={}", quote.getCode(), ex.getMessage());
            log.debug("eastmoney_valuation_failed_stack code={}", quote.getCode(), ex);
        }
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
        return 100;
    }
}
