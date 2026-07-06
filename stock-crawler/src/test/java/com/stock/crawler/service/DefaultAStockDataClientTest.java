package com.stock.crawler.service;

import com.stock.crawler.model.ConceptBlockResult;
import com.stock.crawler.model.DataResult;
import com.stock.crawler.model.StockBasicInfo;
import com.stock.crawler.model.StockQuote;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("a-stock-data 风格能力门面测试")
class DefaultAStockDataClientTest {

    @Test
    @DisplayName("基础信息默认应直接使用腾讯行情字段")
    void getStockBasicInfoUsesTencentQuoteFirst() {
        DefaultAStockDataClient client = new DefaultAStockDataClient(
                new StubStockMarketService(),
                new TechnicalIndicatorService(),
                new FinancialService(),
                new ResearchReportService(),
                new FailingStockIntelligenceService());

        DataResult<StockBasicInfo> result = client.getStockBasicInfo("600519");

        assertTrue(result.isSuccess());
        assertEquals("stock-info:tencent-first", result.getSource());
        assertNotNull(result.getData());
        assertEquals("sh600519", result.getData().getCode());
        assertEquals("贵州茅台", result.getData().getName());
        assertEquals(0, new BigDecimal("1271.10").compareTo(result.getData().getPrice()));
    }

    @Test
    @DisplayName("东财独有能力失败时应返回结构化失败而不是抛异常")
    void eastMoneyOnlyCapabilityReturnsFailureResult() {
        DefaultAStockDataClient client = new DefaultAStockDataClient(
                new StubStockMarketService(),
                new TechnicalIndicatorService(),
                new FinancialService(),
                new ResearchReportService(),
                new FailingStockIntelligenceService());

        DataResult<ConceptBlockResult> result = client.getConceptBlocks("600519");

        assertFalse(result.isSuccess());
        assertEquals("concept-blocks:eastmoney-slist-limited", result.getSource());
        assertTrue(result.getMessage().contains("simulated eastmoney eof"));
    }

    private static class StubStockMarketService extends StockMarketService {
        @Override
        public StockQuote getQuote(String stockCode) {
            return StockQuote.builder()
                    .code("sh600519")
                    .name("贵州茅台")
                    .price(new BigDecimal("1271.10"))
                    .marketCap(new BigDecimal("1588979000000"))
                    .source("Tencent")
                    .build();
        }
    }

    private static class FailingStockIntelligenceService extends StockIntelligenceService {
        @Override
        public StockBasicInfo getStockBasicInfo(String stockCode) throws IOException {
            throw new IOException("simulated eastmoney eof");
        }

        @Override
        public ConceptBlockResult getConceptBlocks(String stockCode) throws IOException {
            throw new IOException("simulated eastmoney eof");
        }
    }
}
