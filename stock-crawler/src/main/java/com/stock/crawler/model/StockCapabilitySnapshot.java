package com.stock.crawler.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 个股综合能力快照。
 */
public class StockCapabilitySnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    private String code;
    private StockQuote quote;
    private StockBasicInfo basicInfo;
    private ConceptBlockResult conceptBlocks;
    private List<FundFlowPoint> minuteFundFlow = new ArrayList<>();
    private List<FundFlowPoint> dailyFundFlow = new ArrayList<>();
    private List<FinancialIndicator> financialIndicators = new ArrayList<>();
    private List<ShareholderConcentration> shareholderConcentrations = new ArrayList<>();
    private List<ResearchReport> researchReports = new ArrayList<>();
    private List<MarketNewsItem> stockNews = new ArrayList<>();
    private List<AnnouncementItem> announcements = new ArrayList<>();

    public StockCapabilitySnapshot() {
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public StockQuote getQuote() { return quote; }
    public void setQuote(StockQuote quote) { this.quote = quote; }
    public StockBasicInfo getBasicInfo() { return basicInfo; }
    public void setBasicInfo(StockBasicInfo basicInfo) { this.basicInfo = basicInfo; }
    public ConceptBlockResult getConceptBlocks() { return conceptBlocks; }
    public void setConceptBlocks(ConceptBlockResult conceptBlocks) { this.conceptBlocks = conceptBlocks; }
    public List<FundFlowPoint> getMinuteFundFlow() { return minuteFundFlow; }
    public void setMinuteFundFlow(List<FundFlowPoint> minuteFundFlow) { this.minuteFundFlow = minuteFundFlow; }
    public List<FundFlowPoint> getDailyFundFlow() { return dailyFundFlow; }
    public void setDailyFundFlow(List<FundFlowPoint> dailyFundFlow) { this.dailyFundFlow = dailyFundFlow; }
    public List<FinancialIndicator> getFinancialIndicators() { return financialIndicators; }
    public void setFinancialIndicators(List<FinancialIndicator> financialIndicators) { this.financialIndicators = financialIndicators; }
    public List<ShareholderConcentration> getShareholderConcentrations() { return shareholderConcentrations; }
    public void setShareholderConcentrations(List<ShareholderConcentration> shareholderConcentrations) { this.shareholderConcentrations = shareholderConcentrations; }
    public List<ResearchReport> getResearchReports() { return researchReports; }
    public void setResearchReports(List<ResearchReport> researchReports) { this.researchReports = researchReports; }
    public List<MarketNewsItem> getStockNews() { return stockNews; }
    public void setStockNews(List<MarketNewsItem> stockNews) { this.stockNews = stockNews; }
    public List<AnnouncementItem> getAnnouncements() { return announcements; }
    public void setAnnouncements(List<AnnouncementItem> announcements) { this.announcements = announcements; }
}
