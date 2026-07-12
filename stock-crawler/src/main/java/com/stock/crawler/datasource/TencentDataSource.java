package com.stock.crawler.datasource;

import com.stock.crawler.exception.MarketDataAccessException;
import com.stock.crawler.model.KLineData;
import com.stock.crawler.model.StockQuote;
import com.stock.crawler.util.CrawlerRequestPolicy;
import com.stock.crawler.util.HttpUtils;
import com.stock.crawler.util.ParseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 腾讯财经数据源
 * 通过 qt.gtimg.cn 获取实时行情，单次请求即可返回完整行情 + 估值数据（PE/PB/市值/换手率）。
 * 编码为 GBK，格式为波浪号分隔的多字段字符串。
 */
public class TencentDataSource implements MarketDataSource {

    private static final Logger log = LoggerFactory.getLogger(TencentDataSource.class);

    private static final String TENCENT_QUOTE_URL = "https://qt.gtimg.cn/q=%s";

    /** 金额单位换算：腾讯金额字段返回万元/亿元，统一转换为元 */
    private static final BigDecimal WAN_TO_YUAN = new BigDecimal("10000");
    private static final BigDecimal YI_TO_YUAN = new BigDecimal("100000000");
    /** 涨跌幅百分比计算分母 */
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final QuoteBodyFetcher quoteBodyFetcher;

    public TencentDataSource() {
        this((url, policy) -> HttpUtils.getWithCharset(url, HttpUtils.GBK, policy));
    }

    TencentDataSource(QuoteBodyFetcher quoteBodyFetcher) {
        this.quoteBodyFetcher = Objects.requireNonNull(quoteBodyFetcher, "quoteBodyFetcher");
    }

    @Override
    public String getName() {
        return "Tencent";
    }

    @Override
    public List<StockQuote> getRealTimeQuotes(List<String> stockCodes) {
        if (stockCodes == null || stockCodes.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            String codesParam = String.join(",", stockCodes);
            String url = String.format(TENCENT_QUOTE_URL, codesParam);
            String body = quoteBodyFetcher.fetch(url, CrawlerRequestPolicy.interactive());
            validateResponse(body);
            return parseTencentQuotes(body);
        } catch (MarketDataAccessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new MarketDataAccessException(
                    getName(),
                    "quote",
                    "Tencent quote request failed for " + stockCodes.size() + " stock code(s)",
                    ex);
        }
    }

    private void validateResponse(String body) {
        if (body == null || body.isBlank()) {
            return;
        }

        for (String line : body.split(";")) {
            if (line == null || line.isBlank()) {
                continue;
            }
            int eqIdx = line.indexOf("=\"");
            int dataStart = eqIdx + 2;
            int dataEnd = line.lastIndexOf('"');
            if (line.startsWith("v_") && eqIdx > 2 && dataEnd >= dataStart) {
                return;
            }
        }

        throw new MarketDataAccessException(
                getName(),
                "quote",
                "Tencent quote response has invalid format",
                null);
    }

    /**
     * 解析腾讯行情数据
     * 格式: v_sh600519="1~贵州茅台~600519~当前价~昨收~今开~...";v_sz000001="...";
     * 字段以 ~ 分隔，关键索引（0-indexed）：
     *   1=名称  2=代码  3=当前价  4=昨收  5=今开  6=成交量(手)
     *   31=涨跌额  32=涨跌幅  33=最高  34=最低  37=成交额(万)
     *   38=换手率  39=PE(TTM)  44=总市值(亿)  46=PB
     */
    private List<StockQuote> parseTencentQuotes(String body) {
        List<StockQuote> quotes = new ArrayList<>();
        String[] lines = body.split(";");

        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            try {
                int eqIdx = line.indexOf("=\"");
                if (eqIdx == -1) {
                    continue;
                }
                // 提取代码：v_sh600519 -> sh600519
                String code = line.substring(2, eqIdx);
                // 提取数据部分（去掉末尾引号）
                int dataStart = eqIdx + 2;
                int dataEnd = line.lastIndexOf("\"");
                if (dataEnd <= dataStart) {
                    continue;
                }
                String data = line.substring(dataStart, dataEnd);
                if (data.isEmpty()) {
                    continue;
                }

                String[] fields = data.split("~", -1);
                if (fields.length <= 34) {
                    log.debug("tencent_quote_fields_too_few code={} fieldCount={}", code, fields.length);
                    continue;
                }

                StockQuote quote = StockQuote.builder()
                        .code(code)
                        .name(fields[1])
                        .price(ParseUtils.parseBigDecimal(fields[3]))
                        .preClose(ParseUtils.parseBigDecimal(fields[4]))
                        .open(ParseUtils.parseBigDecimal(fields[5]))
                        .high(ParseUtils.parseBigDecimal(fields[33]))
                        .low(ParseUtils.parseBigDecimal(fields[34]))
                        .volume(ParseUtils.parseLong(fields[6]) * 100)         // 手 -> 股
                        .amount(parseOptionalBigDecimal(fields, 37, WAN_TO_YUAN)) // 万 -> 元
                        .change(ParseUtils.parseBigDecimal(fields[31]))
                        .changePercent(ParseUtils.parseBigDecimal(fields[32]))
                        .turnoverRate(parseOptionalBigDecimal(fields, 38, null))
                        .pe(parseOptionalBigDecimal(fields, 39, null))
                        .marketCap(parseOptionalBigDecimal(fields, 44, YI_TO_YUAN))
                        .pb(parseOptionalBigDecimal(fields, 46, null))
                        .source("Tencent")
                        .time(LocalDateTime.now())
                        .build();

                // 计算涨跌额和涨跌幅（与 SinaDataSource 逻辑一致）
                if (quote.getPreClose() != null && quote.getPreClose().compareTo(BigDecimal.ZERO) > 0
                        && quote.getPrice() != null) {
                    quote.setChange(quote.getPrice().subtract(quote.getPreClose()));
                    quote.setChangePercent(quote.getChange()
                            .multiply(HUNDRED)
                            .divide(quote.getPreClose(), 2, RoundingMode.HALF_UP));
                }

                quotes.add(quote);
            } catch (RuntimeException ex) {
                log.warn("tencent_quote_parse_failed line={} message={}", line, ex.getMessage(), ex);
            }
        }
        return quotes;
    }

    private BigDecimal parseOptionalBigDecimal(String[] fields, int index, BigDecimal multiplier) {
        if (fields.length <= index || fields[index] == null || fields[index].isBlank()) {
            return null;
        }
        BigDecimal value = ParseUtils.parseBigDecimal(fields[index]);
        return multiplier == null ? value : value.multiply(multiplier);
    }

    @Override
    public List<KLineData> getKLineData(String stockCode, String period, int days) {
        // 腾讯数据源不支持 K 线，由 Baidu/Sina/EastMoney 数据源兜底
        return new ArrayList<>();
    }

    @Override
    public boolean supportsKLinePeriod(String period) {
        return false;
    }

    @Override
    public List<StockQuote> searchStock(String keyword) {
        // 腾讯数据源不支持搜索，由 SinaDataSource 实现
        return new ArrayList<>();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @FunctionalInterface
    interface QuoteBodyFetcher {
        String fetch(String url, CrawlerRequestPolicy policy) throws IOException;
    }
}
