package com.stock.web.service;

import com.stock.crawler.model.AnnouncementItem;
import com.stock.crawler.model.ConceptBlockResult;
import com.stock.crawler.model.DataResult;
import com.stock.crawler.model.FundFlowPoint;
import com.stock.crawler.model.MarketNewsItem;
import com.stock.crawler.model.StockCapabilitySnapshot;
import com.stock.crawler.model.StockBasicInfo;
import com.stock.crawler.model.StockQuote;
import com.stock.crawler.service.AStockDataClient;
import com.stock.crawler.service.DefaultAStockDataClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * stock-crawler 能力门面 Web 包装。
 */
@Service
public class CapabilityWebService {

    private final AStockDataClient client;

    public CapabilityWebService() {
        this(new DefaultAStockDataClient());
    }

    CapabilityWebService(AStockDataClient client) {
        this.client = client;
    }

    public DataResult<StockQuote> getQuote(String stockCode) {
        return client.getQuote(stockCode);
    }

    public DataResult<StockCapabilitySnapshot> getSnapshot(String stockCode) {
        return client.getCapabilitySnapshot(stockCode);
    }

    public DataResult<StockBasicInfo> getStockBasicInfo(String stockCode) {
        return client.getStockBasicInfo(stockCode);
    }

    public DataResult<ConceptBlockResult> getConceptBlocks(String stockCode) {
        return client.getConceptBlocks(stockCode);
    }

    public DataResult<List<FundFlowPoint>> getFundFlowMinute(String stockCode) {
        return client.getFundFlowMinute(stockCode);
    }

    public DataResult<List<FundFlowPoint>> getFundFlowDaily(String stockCode, int limit) {
        return client.getFundFlowDaily(stockCode, limit);
    }

    public DataResult<List<MarketNewsItem>> getStockNews(String stockCode, int limit) {
        return client.getStockNews(stockCode, limit);
    }

    public DataResult<List<MarketNewsItem>> getGlobalNews(int limit) {
        return client.getGlobalNews(limit);
    }

    public DataResult<List<AnnouncementItem>> getAnnouncements(String stockCode, int limit) {
        return client.getAnnouncements(stockCode, limit);
    }
}
