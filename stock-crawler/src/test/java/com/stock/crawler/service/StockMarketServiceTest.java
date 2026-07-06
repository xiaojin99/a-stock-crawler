package com.stock.crawler.service;

import com.stock.crawler.datasource.EastMoneyDataSource;
import com.stock.crawler.datasource.MarketDataSource;
import com.stock.crawler.model.KLineData;
import com.stock.crawler.model.StockQuote;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("股票行情聚合服务测试")
class StockMarketServiceTest {

    @Test
    @DisplayName("实时行情缺少估值字段时不应同步调用东财补充")
    void quoteHotPathDoesNotSynchronouslyEnrichValuationWithEastMoney() {
        TrackingEastMoneyDataSource eastMoneyDataSource = new TrackingEastMoneyDataSource();
        StockMarketService service = new StockMarketService(
                List.of(new QuoteOnlyDataSource(), eastMoneyDataSource),
                new TechnicalIndicatorService());

        StockQuote quote = service.getQuote("600519");

        assertEquals("sh600519", quote.getCode());
        assertEquals("Sina", quote.getSource());
        assertEquals(0, eastMoneyDataSource.enrichCallCount);
    }

    @Test
    @DisplayName("批量行情应对同一数据源发起一次批量请求")
    void batchQuotesUseOneDataSourceCallForCacheMisses() {
        BatchQuoteDataSource dataSource = new BatchQuoteDataSource();
        StockMarketService service = new StockMarketService(
                List.of(dataSource),
                new TechnicalIndicatorService());

        List<StockQuote> quotes = service.getRealTimeQuotes(List.of("600519", "000001"));

        assertEquals(1, dataSource.callCount);
        assertEquals(List.of("sh600519", "sz000001"), dataSource.requestedCodes);
        assertEquals(2, quotes.size());
        assertEquals("sh600519", quotes.get(0).getCode());
        assertEquals("sz000001", quotes.get(1).getCode());
    }

    private static class QuoteOnlyDataSource implements MarketDataSource {
        @Override
        public String getName() {
            return "QuoteOnly";
        }

        @Override
        public List<StockQuote> getRealTimeQuotes(List<String> stockCodes) {
            return List.of(StockQuote.builder()
                    .code("sh600519")
                    .name("贵州茅台")
                    .price(new BigDecimal("1271.10"))
                    .source("Sina")
                    .build());
        }

        @Override
        public List<KLineData> getKLineData(String stockCode, String period, int days) {
            return new ArrayList<>();
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
            return 1;
        }
    }

    private static class TrackingEastMoneyDataSource extends EastMoneyDataSource {
        private int enrichCallCount;

        @Override
        public void enrichValuation(StockQuote quote) {
            enrichCallCount++;
        }

        @Override
        public int getPriority() {
            return 100;
        }
    }

    private static class BatchQuoteDataSource implements MarketDataSource {
        private int callCount;
        private List<String> requestedCodes = new ArrayList<>();

        @Override
        public String getName() {
            return "BatchQuote";
        }

        @Override
        public List<StockQuote> getRealTimeQuotes(List<String> stockCodes) {
            callCount++;
            requestedCodes = new ArrayList<>(stockCodes);
            return stockCodes.stream()
                    .map(code -> StockQuote.builder()
                            .code(code)
                            .name(code)
                            .price(BigDecimal.ONE)
                            .source("BatchQuote")
                            .build())
                    .toList();
        }

        @Override
        public List<KLineData> getKLineData(String stockCode, String period, int days) {
            return new ArrayList<>();
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
            return 1;
        }
    }
}
