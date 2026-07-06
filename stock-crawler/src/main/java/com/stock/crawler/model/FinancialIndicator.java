package com.stock.crawler.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 个股主要财务指标
 * 数据来源：东方财富 RPT_F10_FINANCE_MAINFINADATA
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FinancialIndicator {

    private String securityCode;
    private String securityNameAbbr;
    private String reportDate;
    private String reportDateName;

    /** 营业收入（元） */
    private Double totalOperateIncome;

    /** 营业收入同比增长（%） */
    private Double totalOperateIncomeYoy;

    /** 净利润（元） */
    private Double parentNetprofit;

    /** 净利润同比增长（%） */
    private Double parentNetprofitYoy;

    /** 扣非净利润（元） */
    private Double cutParentNetprofit;

    /** 扣非净利润同比增长（%） */
    private Double cutParentNetprofitYoy;

    /** 毛利率（%） */
    private Double grossProfitMargin;

    /** 净利率（%） */
    private Double netProfitMargin;

    /** ROE（加权）（%） */
    private Double roeWeighted;

    /** 每股收益（元） */
    private Double basicEps;

    /** 每股净资产（元） */
    private Double bps;

    /** 资产负债率（%） */
    private Double debtAssetRatio;

    /** 经营现金流净额（元） */
    private Double operateCashFlowPs;

    public FinancialIndicator() {
    }

    public String getSecurityCode() { return securityCode; }
    public void setSecurityCode(String securityCode) { this.securityCode = securityCode; }

    public String getSecurityNameAbbr() { return securityNameAbbr; }
    public void setSecurityNameAbbr(String securityNameAbbr) { this.securityNameAbbr = securityNameAbbr; }

    public String getReportDate() { return reportDate; }
    public void setReportDate(String reportDate) { this.reportDate = reportDate; }

    public String getReportDateName() { return reportDateName; }
    public void setReportDateName(String reportDateName) { this.reportDateName = reportDateName; }

    public Double getTotalOperateIncome() { return totalOperateIncome; }
    public void setTotalOperateIncome(Double totalOperateIncome) { this.totalOperateIncome = totalOperateIncome; }

    public Double getTotalOperateIncomeYoy() { return totalOperateIncomeYoy; }
    public void setTotalOperateIncomeYoy(Double totalOperateIncomeYoy) { this.totalOperateIncomeYoy = totalOperateIncomeYoy; }

    public Double getParentNetprofit() { return parentNetprofit; }
    public void setParentNetprofit(Double parentNetprofit) { this.parentNetprofit = parentNetprofit; }

    public Double getParentNetprofitYoy() { return parentNetprofitYoy; }
    public void setParentNetprofitYoy(Double parentNetprofitYoy) { this.parentNetprofitYoy = parentNetprofitYoy; }

    public Double getCutParentNetprofit() { return cutParentNetprofit; }
    public void setCutParentNetprofit(Double cutParentNetprofit) { this.cutParentNetprofit = cutParentNetprofit; }

    public Double getCutParentNetprofitYoy() { return cutParentNetprofitYoy; }
    public void setCutParentNetprofitYoy(Double cutParentNetprofitYoy) { this.cutParentNetprofitYoy = cutParentNetprofitYoy; }

    public Double getGrossProfitMargin() { return grossProfitMargin; }
    public void setGrossProfitMargin(Double grossProfitMargin) { this.grossProfitMargin = grossProfitMargin; }

    public Double getNetProfitMargin() { return netProfitMargin; }
    public void setNetProfitMargin(Double netProfitMargin) { this.netProfitMargin = netProfitMargin; }

    public Double getRoeWeighted() { return roeWeighted; }
    public void setRoeWeighted(Double roeWeighted) { this.roeWeighted = roeWeighted; }

    public Double getBasicEps() { return basicEps; }
    public void setBasicEps(Double basicEps) { this.basicEps = basicEps; }

    public Double getBps() { return bps; }
    public void setBps(Double bps) { this.bps = bps; }

    public Double getDebtAssetRatio() { return debtAssetRatio; }
    public void setDebtAssetRatio(Double debtAssetRatio) { this.debtAssetRatio = debtAssetRatio; }

    public Double getOperateCashFlowPs() { return operateCashFlowPs; }
    public void setOperateCashFlowPs(Double operateCashFlowPs) { this.operateCashFlowPs = operateCashFlowPs; }

    @Override
    public String toString() {
        return "FinancialIndicator{" +
                "securityCode='" + securityCode + '\'' +
                ", reportDateName='" + reportDateName + '\'' +
                ", revenue=" + totalOperateIncome +
                ", netProfit=" + parentNetprofit +
                ", roe=" + roeWeighted +
                '}';
    }
}
