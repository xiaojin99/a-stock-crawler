package com.stock.crawler.service;

import com.stock.crawler.model.AnnouncementItem;
import com.stock.crawler.model.ConceptBlockResult;
import com.stock.crawler.model.DataResult;
import com.stock.crawler.model.FinancialIndicator;
import com.stock.crawler.model.FundFlowPoint;
import com.stock.crawler.model.KLineData;
import com.stock.crawler.model.MarketNewsItem;
import com.stock.crawler.model.ResearchReport;
import com.stock.crawler.model.ShareholderConcentration;
import com.stock.crawler.model.StockBasicInfo;
import com.stock.crawler.model.StockCapabilitySnapshot;
import com.stock.crawler.model.StockQuote;
import com.stock.crawler.model.TechnicalIndicators;
import com.stock.crawler.model.TradingCalendarSnapshot;

import java.time.YearMonth;
import java.util.List;

/**
 * stock-crawler 对上层系统暴露的统一能力门面。
 */
public interface AStockDataClient {

    DataResult<StockQuote> getQuote(String stockCode);

    DataResult<List<StockQuote>> getQuotes(List<String> stockCodes);

    DataResult<List<KLineData>> getKLineData(String stockCode, String period, int days);

    DataResult<TradingCalendarSnapshot> getTradingCalendar(YearMonth month);

    DataResult<TechnicalIndicators> getTechnicalIndicators(String stockCode, int days);

    DataResult<StockBasicInfo> getStockBasicInfo(String stockCode);

    DataResult<ConceptBlockResult> getConceptBlocks(String stockCode);

    DataResult<List<FundFlowPoint>> getFundFlowMinute(String stockCode);

    DataResult<List<FundFlowPoint>> getFundFlowDaily(String stockCode, int limit);

    DataResult<List<FinancialIndicator>> getFinancialIndicators(String stockCode, int reportCount);

    DataResult<List<ShareholderConcentration>> getShareholderConcentration(String stockCode, int reportCount);

    DataResult<List<ResearchReport>> getResearchReports(String stockCode, int pageSize);

    DataResult<List<MarketNewsItem>> getStockNews(String stockCode, int pageSize);

    DataResult<List<MarketNewsItem>> getGlobalNews(int pageSize);

    DataResult<List<AnnouncementItem>> getAnnouncements(String stockCode, int pageSize);

    DataResult<StockCapabilitySnapshot> getCapabilitySnapshot(String stockCode);
}
