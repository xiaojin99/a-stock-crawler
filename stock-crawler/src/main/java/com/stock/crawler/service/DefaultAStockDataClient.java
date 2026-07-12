package com.stock.crawler.service;

import com.stock.crawler.model.AnnouncementItem;
import com.stock.crawler.model.ConceptBlockResult;
import com.stock.crawler.model.DataResult;
import com.stock.crawler.model.FinancialIndicator;
import com.stock.crawler.model.FundFlowPoint;
import com.stock.crawler.model.KLineData;
import com.stock.crawler.model.MarketBreadthSnapshot;
import com.stock.crawler.model.MarketNewsItem;
import com.stock.crawler.model.ResearchReport;
import com.stock.crawler.model.ShareholderConcentration;
import com.stock.crawler.model.StockBasicInfo;
import com.stock.crawler.model.StockCapabilitySnapshot;
import com.stock.crawler.model.StockQuote;
import com.stock.crawler.model.TechnicalIndicators;
import com.stock.crawler.model.TradingCalendarSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * 默认 a-stock-data 风格能力门面实现。
 */
public class DefaultAStockDataClient implements AStockDataClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultAStockDataClient.class);
    private static final boolean EASTMONEY_STOCK_BASIC_ENRICHMENT_ENABLED =
            Boolean.getBoolean("stockcrawler.stockBasic.enrichEastMoney");

    private final StockMarketService stockMarketService;
    private final TechnicalIndicatorService technicalIndicatorService;
    private final FinancialService financialService;
    private final ResearchReportService researchReportService;
    private final StockIntelligenceService stockIntelligenceService;
    private final TradingCalendarService tradingCalendarService;
    private final MarketBreadthService marketBreadthService;

    public DefaultAStockDataClient() {
        this(new StockMarketService(), new TechnicalIndicatorService(), new FinancialService(),
                new ResearchReportService(), new StockIntelligenceService(),
                new TradingCalendarService(), new MarketBreadthService());
    }

    public DefaultAStockDataClient(
            StockMarketService stockMarketService,
            TechnicalIndicatorService technicalIndicatorService,
            FinancialService financialService,
            ResearchReportService researchReportService,
            StockIntelligenceService stockIntelligenceService) {
        this(stockMarketService, technicalIndicatorService, financialService,
                researchReportService, stockIntelligenceService, new TradingCalendarService(),
                new MarketBreadthService());
    }

    public DefaultAStockDataClient(
            StockMarketService stockMarketService,
            TechnicalIndicatorService technicalIndicatorService,
            FinancialService financialService,
            ResearchReportService researchReportService,
            StockIntelligenceService stockIntelligenceService,
            TradingCalendarService tradingCalendarService) {
        this(stockMarketService, technicalIndicatorService, financialService,
                researchReportService, stockIntelligenceService, tradingCalendarService,
                new MarketBreadthService());
    }

    public DefaultAStockDataClient(
            StockMarketService stockMarketService,
            TechnicalIndicatorService technicalIndicatorService,
            FinancialService financialService,
            ResearchReportService researchReportService,
            StockIntelligenceService stockIntelligenceService,
            TradingCalendarService tradingCalendarService,
            MarketBreadthService marketBreadthService) {
        this.stockMarketService = stockMarketService;
        this.technicalIndicatorService = technicalIndicatorService;
        this.financialService = financialService;
        this.researchReportService = researchReportService;
        this.stockIntelligenceService = stockIntelligenceService;
        this.tradingCalendarService = tradingCalendarService;
        this.marketBreadthService = marketBreadthService;
    }

    @Override
    public DataResult<StockQuote> getQuote(String stockCode) {
        return wrap("quote:tencent-first", () -> stockMarketService.getQuote(stockCode));
    }

    @Override
    public DataResult<List<StockQuote>> getQuotes(List<String> stockCodes) {
        return wrap("quotes:tencent-first", () -> stockMarketService.getRealTimeQuotes(stockCodes));
    }

    @Override
    public DataResult<List<KLineData>> getKLineData(String stockCode, String period, int days) {
        return wrap("kline:baidu-tencent-eastmoney-fallback",
                () -> stockMarketService.getKLineData(stockCode, period, days));
    }

    @Override
    public DataResult<TradingCalendarSnapshot> getTradingCalendar(YearMonth month) {
        return tradingCalendarService.getTradingCalendar(month);
    }

    @Override
    public DataResult<MarketBreadthSnapshot> getMarketBreadth() {
        return marketBreadthService.getMarketBreadth();
    }

    @Override
    public DataResult<TechnicalIndicators> getTechnicalIndicators(String stockCode, int days) {
        return wrap("technical:local-ta4j", () -> {
            List<KLineData> klineData = stockMarketService.getKLineData(stockCode, "day", days);
            if (klineData == null || klineData.isEmpty()) {
                return null;
            }
            return technicalIndicatorService.calculate(klineData);
        });
    }

    @Override
    public DataResult<StockBasicInfo> getStockBasicInfo(String stockCode) {
        long startedAt = System.nanoTime();
        StockQuote quote = null;
        try {
            quote = stockMarketService.getQuote(stockCode);
        } catch (Exception ex) {
            log.warn("stock_info_tencent_quote_failed fallback=eastmoney stockCode={} message={}",
                    stockCode, ex.getMessage());
        }

        if (quote != null) {
            StockBasicInfo info = toBasicInfo(quote);
            if (EASTMONEY_STOCK_BASIC_ENRICHMENT_ENABLED) {
                enrichStockBasicInfo(stockCode, info);
            }
            return DataResult.success(info,
                    EASTMONEY_STOCK_BASIC_ENRICHMENT_ENABLED
                            ? "stock-info:tencent-eastmoney-enriched"
                            : "stock-info:tencent-first",
                    elapsedMs(startedAt));
        }

        try {
            StockBasicInfo info = stockIntelligenceService.getStockBasicInfo(stockCode);
            DataResult<StockBasicInfo> result = DataResult.success(info,
                    "stock-info:eastmoney-limited-fallback", elapsedMs(startedAt));
            result.setMessage("Tencent quote unavailable, fallback to EastMoney stock info");
            return result;
        } catch (Exception ex) {
            long elapsedMs = elapsedMs(startedAt);
            log.warn("stock_info_all_sources_failed stockCode={} elapsedMs={} message={}",
                    stockCode, elapsedMs, ex.getMessage());
            return DataResult.failure("stock-info:tencent-eastmoney-fallback", ex.getMessage(), elapsedMs);
        }
    }

    private void enrichStockBasicInfo(String stockCode, StockBasicInfo info) {
        try {
            StockBasicInfo eastMoneyInfo = stockIntelligenceService.getStockBasicInfo(stockCode);
            mergeStockBasicInfo(info, eastMoneyInfo);
        } catch (Exception ex) {
            log.warn("stock_info_eastmoney_enrichment_failed stockCode={} message={}",
                    stockCode, ex.getMessage());
        }
    }

    private void mergeStockBasicInfo(StockBasicInfo target, StockBasicInfo source) {
        if (target == null || source == null) {
            return;
        }
        if (target.getName() == null) {
            target.setName(source.getName());
        }
        if (target.getIndustry() == null) {
            target.setIndustry(source.getIndustry());
        }
        if (target.getTotalShares() == null) {
            target.setTotalShares(source.getTotalShares());
        }
        if (target.getFloatShares() == null) {
            target.setFloatShares(source.getFloatShares());
        }
        if (target.getMarketCap() == null) {
            target.setMarketCap(source.getMarketCap());
        }
        if (target.getFloatMarketCap() == null) {
            target.setFloatMarketCap(source.getFloatMarketCap());
        }
        if (target.getListDate() == null) {
            target.setListDate(source.getListDate());
        }
        if (target.getPrice() == null) {
            target.setPrice(source.getPrice());
        }
    }

    @Override
    public DataResult<ConceptBlockResult> getConceptBlocks(String stockCode) {
        return wrap("concept-blocks:eastmoney-slist-limited",
                () -> stockIntelligenceService.getConceptBlocks(stockCode));
    }

    @Override
    public DataResult<List<FundFlowPoint>> getFundFlowMinute(String stockCode) {
        return wrap("fund-flow-minute:eastmoney-push2-limited",
                () -> stockIntelligenceService.getFundFlowMinute(stockCode));
    }

    @Override
    public DataResult<List<FundFlowPoint>> getFundFlowDaily(String stockCode, int limit) {
        return wrap("fund-flow-daily:eastmoney-sina-fallback",
                () -> stockIntelligenceService.getFundFlowDaily(stockCode, limit));
    }

    @Override
    public DataResult<List<FinancialIndicator>> getFinancialIndicators(String stockCode, int reportCount) {
        return wrap("financial:eastmoney-datacenter-limited",
                () -> financialService.getMainFinancialIndicators(stockCode, reportCount));
    }

    @Override
    public DataResult<List<ShareholderConcentration>> getShareholderConcentration(String stockCode, int reportCount) {
        return wrap("shareholders:eastmoney-datacenter-limited",
                () -> financialService.getShareholderConcentration(stockCode, reportCount));
    }

    @Override
    public DataResult<List<ResearchReport>> getResearchReports(String stockCode, int pageSize) {
        return wrap("research:eastmoney-reportapi-limited",
                () -> researchReportService.getResearchReports(stockCode, pageSize, 1).getData());
    }

    @Override
    public DataResult<List<MarketNewsItem>> getStockNews(String stockCode, int pageSize) {
        return wrap("stock-news:eastmoney-search-limited",
                () -> stockIntelligenceService.getStockNews(stockCode, pageSize));
    }

    @Override
    public DataResult<List<MarketNewsItem>> getGlobalNews(int pageSize) {
        return wrap("global-news:eastmoney-724-limited",
                () -> stockIntelligenceService.getGlobalNews(pageSize));
    }

    @Override
    public DataResult<List<AnnouncementItem>> getAnnouncements(String stockCode, int pageSize) {
        return wrap("announcements:cninfo", () -> stockIntelligenceService.getAnnouncements(stockCode, pageSize));
    }

    @Override
    public DataResult<StockCapabilitySnapshot> getCapabilitySnapshot(String stockCode) {
        return wrap("snapshot:composite", () -> {
            StockCapabilitySnapshot snapshot = new StockCapabilitySnapshot();
            snapshot.setCode(stockCode);
            snapshot.setQuote(nullOnFailure(getQuote(stockCode)));
            snapshot.setBasicInfo(nullOnFailure(getStockBasicInfo(stockCode)));
            snapshot.setConceptBlocks(nullOnFailure(getConceptBlocks(stockCode)));
            snapshot.setMinuteFundFlow(listOnFailure(getFundFlowMinute(stockCode)));
            snapshot.setDailyFundFlow(listOnFailure(getFundFlowDaily(stockCode, 20)));
            snapshot.setFinancialIndicators(listOnFailure(getFinancialIndicators(stockCode, 6)));
            snapshot.setShareholderConcentrations(listOnFailure(getShareholderConcentration(stockCode, 6)));
            snapshot.setResearchReports(listOnFailure(getResearchReports(stockCode, 5)));
            snapshot.setStockNews(listOnFailure(getStockNews(stockCode, 8)));
            snapshot.setAnnouncements(listOnFailure(getAnnouncements(stockCode, 8)));
            return snapshot;
        });
    }

    private <T> DataResult<T> wrap(String source, CheckedSupplier<T> supplier) {
        long startedAt = System.nanoTime();
        try {
            T data = supplier.get();
            long elapsedMs = elapsedMs(startedAt);
            if (data == null) {
                return DataResult.failure(source, "No data returned from " + source, elapsedMs);
            }
            return DataResult.success(data, source, elapsedMs);
        } catch (Exception ex) {
            long elapsedMs = elapsedMs(startedAt);
            log.warn("stock_data_capability_failed source={} elapsedMs={} message={}",
                    source, elapsedMs, ex.getMessage());
            return DataResult.failure(source, ex.getMessage(), elapsedMs);
        }
    }

    private <T> T nullOnFailure(DataResult<T> result) {
        return result != null && result.isSuccess() ? result.getData() : null;
    }

    private <T> List<T> listOnFailure(DataResult<List<T>> result) {
        return result != null && result.isSuccess() && result.getData() != null
                ? result.getData()
                : new ArrayList<>();
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private StockBasicInfo toBasicInfo(StockQuote quote) {
        StockBasicInfo info = new StockBasicInfo();
        info.setCode(quote.getCode());
        info.setName(quote.getName());
        info.setPrice(quote.getPrice());
        info.setMarketCap(quote.getMarketCap());
        return info;
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}
