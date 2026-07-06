package com.stock.crawler.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 个股人气榜条目
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockRankItem {

    private String stockCode;       // 股票代码 (如 "SH601016")
    private Integer rank;           // 当前排名
    private Integer rankChange;     // 排名变化 (1:上升, -1:下降, 0:不变)
    private Integer histRankChange; // 历史排名变化
    private String stockName;       // 股票名称
    private Double closePrice;      // 收盘价
    private Double changePercent;   // 涨跌幅

    public StockRankItem() {
    }

    // Getters and Setters
    public String getStockCode() {
        return stockCode;
    }

    public void setStockCode(String stockCode) {
        this.stockCode = stockCode;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public Integer getRankChange() {
        return rankChange;
    }

    public void setRankChange(Integer rankChange) {
        this.rankChange = rankChange;
    }

    public Integer getHistRankChange() {
        return histRankChange;
    }

    public void setHistRankChange(Integer histRankChange) {
        this.histRankChange = histRankChange;
    }

    public String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public Double getClosePrice() {
        return closePrice;
    }

    public void setClosePrice(Double closePrice) {
        this.closePrice = closePrice;
    }

    public Double getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(Double changePercent) {
        this.changePercent = changePercent;
    }

    /**
     * 获取纯股票代码（去掉前缀）
     */
    public String getPureCode() {
        if (stockCode == null) return null;
        if (stockCode.startsWith("SH") || stockCode.startsWith("SZ")) {
            return stockCode.substring(2);
        }
        return stockCode;
    }

    /**
     * 获取市场标识
     */
    public String getMarket() {
        if (stockCode == null) return null;
        if (stockCode.startsWith("SH")) return "sh";
        if (stockCode.startsWith("SZ")) return "sz";
        return "";
    }

    /**
     * 获取排名变化描述
     */
    public String getRankChangeDesc() {
        if (rankChange == null) return "不变";
        if (rankChange > 0) return "上升";
        if (rankChange < 0) return "下降";
        return "不变";
    }

    @Override
    public String toString() {
        return "StockRankItem{" +
                "rank=" + rank +
                ", stockCode='" + stockCode + '\'' +
                ", stockName='" + stockName + '\'' +
                ", rankChange=" + getRankChangeDesc() +
                '}';
    }
}
