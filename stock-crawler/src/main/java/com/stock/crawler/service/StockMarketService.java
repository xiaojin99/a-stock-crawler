package com.stock.crawler.service;

import com.stock.crawler.datasource.BaiduKLineDataSource;
import com.stock.crawler.datasource.EastMoneyDataSource;
import com.stock.crawler.datasource.MarketDataSource;
import com.stock.crawler.datasource.SinaDataSource;
import com.stock.crawler.datasource.TencentDataSource;
import com.stock.crawler.model.KLineData;
import com.stock.crawler.model.StockQuote;
import com.stock.crawler.model.TechnicalIndicators;
import com.stock.crawler.util.StockCodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 股票行情聚合服务
 * 汇聚多个数据源，提供实时行情、K线、搜索、技术指标的统一入口。
 * 包含轻量级内存缓存（TTL 控制），Redis 缓存在 agent-invest Spring 层叠加。
 */
public class StockMarketService {

    private static final Logger log = LoggerFactory.getLogger(StockMarketService.class);

    private static final long QUOTE_CACHE_TTL_MS = 5_000L;   // 实时行情 5 秒
    private static final long KLINE_CACHE_TTL_MS = 300_000L;  // K线 5 分钟

    private final List<MarketDataSource> dataSources;
    private final TechnicalIndicatorService technicalIndicatorService;

    private final Map<String, CacheEntry<StockQuote>> quoteCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry<List<KLineData>>> klineCache = new ConcurrentHashMap<>();

    /**
     * 默认构造：按低封禁风险优先使用腾讯、百度 K 线、新浪，东方财富仅作末位兜底。
     */
    public StockMarketService() {
        EastMoneyDataSource emDs = new EastMoneyDataSource();
        this.dataSources = Arrays.asList(
                        new TencentDataSource(),
                        new BaiduKLineDataSource(),
                        new SinaDataSource(),
                        emDs
                ).stream()
                .sorted(Comparator.comparingInt(MarketDataSource::getPriority))
                .toList();
        this.technicalIndicatorService = new TechnicalIndicatorService();
    }

    /**
     * 自定义构造：支持注入自定义数据源列表（用于 Spring @Bean）
     */
    public StockMarketService(List<MarketDataSource> dataSources,
                               TechnicalIndicatorService technicalIndicatorService) {
        this.dataSources = dataSources.stream()
                .sorted(Comparator.comparingInt(MarketDataSource::getPriority))
                .toList();
        this.technicalIndicatorService = technicalIndicatorService;
    }

    /**
     * 批量获取实时行情
     */
    public List<StockQuote> getRealTimeQuotes(List<String> stockCodes) {
        if (stockCodes == null || stockCodes.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> normalizedCodes = stockCodes.stream()
                .filter(Objects::nonNull)
                .map(this::normalizeStockCode)
                .filter(code -> !code.isBlank())
                .toList();

        Map<String, StockQuote> quoteByCode = new LinkedHashMap<>();
        List<String> cacheMissCodes = new ArrayList<>();

        for (String code : normalizedCodes) {
            CacheEntry<StockQuote> entry = quoteCache.get(code);
            if (entry != null && !entry.isExpired()) {
                quoteByCode.put(code, entry.value);
            } else {
                cacheMissCodes.add(code);
            }
        }

        if (!cacheMissCodes.isEmpty()) {
            for (StockQuote quote : fetchQuotesFromDataSources(cacheMissCodes)) {
                String normalizedQuoteCode = normalizeStockCode(quote.getCode());
                if (!normalizedQuoteCode.isBlank()) {
                    quote.setCode(normalizedQuoteCode);
                    quoteByCode.put(normalizedQuoteCode, quote);
                    quoteCache.put(normalizedQuoteCode, new CacheEntry<>(quote, QUOTE_CACHE_TTL_MS));
                }
            }
        }
        List<StockQuote> results = new ArrayList<>();
        for (String code : normalizedCodes) {
            StockQuote quote = quoteByCode.get(code);
            if (quote != null) {
                results.add(quote);
            }
        }
        return results;
    }

    /**
     * 获取单只股票实时行情
     */
    public StockQuote getQuote(String stockCode) {
        String normalizedCode = normalizeStockCode(stockCode);
        List<StockQuote> quotes = getRealTimeQuotes(List.of(normalizedCode));
        return quotes.isEmpty() ? null : quotes.get(0);
    }

    /**
     * 获取 K 线数据
     */
    public List<KLineData> getKLineData(String stockCode, String period, int days) {
        String normalizedCode = normalizeStockCode(stockCode);
        String normalizedPeriod = normalizePeriod(period);
        String cacheKey = normalizedCode + ":" + normalizedPeriod + ":" + days;

        CacheEntry<List<KLineData>> entry = klineCache.get(cacheKey);
        if (entry != null && !entry.isExpired()) {
            return entry.value;
        }

        List<KLineData> klines = fetchKLineFromDataSource(normalizedCode, normalizedPeriod, days);
        if (klines != null && !klines.isEmpty()) {
            klineCache.put(cacheKey, new CacheEntry<>(klines, KLINE_CACHE_TTL_MS));
            return klines;
        }
        return new ArrayList<>();
    }

    /**
     * 获取技术指标
     */
    public TechnicalIndicators getTechnicalIndicators(String stockCode, int days) {
        List<KLineData> klineData = getKLineData(normalizeStockCode(stockCode), "day", days);
        if (klineData.isEmpty()) {
            return null;
        }
        return technicalIndicatorService.calculate(klineData);
    }

    /**
     * 搜索股票
     */
    public List<StockQuote> searchStock(String keyword) {
        for (MarketDataSource dataSource : dataSources) {
            if (!dataSource.isAvailable()) {
                continue;
            }
            try {
                List<StockQuote> results = dataSource.searchStock(keyword);
                if (!results.isEmpty()) {
                    return results;
                }
            } catch (RuntimeException ex) {
                log.warn("market_data_source_search_failed source={} keyword={} message={}",
                        dataSource.getName(), keyword, ex.getMessage(), ex);
            }
        }
        return new ArrayList<>();
    }

    private List<StockQuote> fetchQuotesFromDataSources(List<String> stockCodes) {
        Set<String> unresolvedCodes = new LinkedHashSet<>(stockCodes);
        List<StockQuote> results = new ArrayList<>();
        for (MarketDataSource dataSource : dataSources) {
            if (unresolvedCodes.isEmpty()) {
                break;
            }
            if (!dataSource.isAvailable()) {
                continue;
            }
            try {
                List<String> requestCodes = new ArrayList<>(unresolvedCodes);
                List<StockQuote> quotes = dataSource.getRealTimeQuotes(requestCodes);
                for (StockQuote quote : quotes) {
                    if (quote == null || quote.getCode() == null) {
                        continue;
                    }
                    String normalizedQuoteCode = normalizeStockCode(quote.getCode());
                    if (unresolvedCodes.remove(normalizedQuoteCode)) {
                        results.add(quote);
                    }
                }
            } catch (RuntimeException ex) {
                log.warn("market_data_source_quote_failed source={} stockCodes={} message={}",
                        dataSource.getName(), unresolvedCodes, ex.getMessage(), ex);
            }
        }
        return results;
    }

    private List<KLineData> fetchKLineFromDataSource(String stockCode, String period, int days) {
        for (MarketDataSource dataSource : dataSources) {
            if (!dataSource.isAvailable()) {
                continue;
            }
            try {
                List<KLineData> klines = dataSource.getKLineData(stockCode, period, days);
                if (!klines.isEmpty()) {
                    return klines;
                }
            } catch (RuntimeException ex) {
                log.warn("market_data_source_kline_failed source={} stockCode={} period={} days={} message={}",
                        dataSource.getName(), stockCode, period, days, ex.getMessage(), ex);
            }
        }
        return new ArrayList<>();
    }

    private String normalizeStockCode(String stockCode) {
        return StockCodeUtils.normalizeWithMarket(stockCode);
    }

    private String normalizePeriod(String period) {
        if (period == null) {
            return "day";
        }
        return switch (period.trim().toLowerCase()) {
            case "day", "daily", "d" -> "day";
            case "week", "weekly", "w" -> "week";
            case "month", "monthly", "m" -> "month";
            default -> "day";
        };
    }

    private static class CacheEntry<T> {
        final T value;
        final long expireAt;

        CacheEntry(T value, long ttlMs) {
            this.value = value;
            this.expireAt = Instant.now().toEpochMilli() + ttlMs;
        }

        boolean isExpired() {
            return Instant.now().toEpochMilli() > expireAt;
        }
    }
}
