package com.stock.web.service;

import com.stock.crawler.model.KLineData;
import com.stock.crawler.model.StockQuote;
import com.stock.crawler.model.TechnicalIndicators;
import com.stock.crawler.service.StockMarketService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 股票行情 Web 服务 - 委托给 stock-crawler 的 StockMarketService
 */
@Service
public class StockMarketWebService {

    private final StockMarketService delegate = new StockMarketService();

    public StockQuote getQuote(String stockCode) {
        return delegate.getQuote(stockCode);
    }

    public List<StockQuote> getRealTimeQuotes(List<String> stockCodes) {
        return delegate.getRealTimeQuotes(stockCodes);
    }

    public List<StockQuote> searchStock(String keyword) {
        return delegate.searchStock(keyword);
    }

    public List<KLineData> getKLineData(String stockCode, String period, int days) {
        return delegate.getKLineData(stockCode, period, days);
    }

    public TechnicalIndicators getTechnicalIndicators(String stockCode, int days) {
        return delegate.getTechnicalIndicators(stockCode, days);
    }
}
