package com.stock.crawler.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 龙虎榜条目数据结构
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LongHuBangItem {

    private String tradeDate;
    private String code;
    private String secuCode;
    private String name;
    private Double closePrice;
    private Double changePercent;
    private Double netBuyAmt;
    private Double buyAmt;
    private Double sellAmt;
    private Double totalAmt;
    private Double turnoverRate;
    private Double freeCap;
    private String reason;
    private String reasonDetail;
    private Double accumAmount;
    private Double dealRatio;
    private Double netRatio;
    private Double d1Change;
    private Double d2Change;
    private Double d5Change;
    private Double d10Change;

    public LongHuBangItem() {
    }

    // Getters and Setters
    public String getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(String tradeDate) {
        this.tradeDate = tradeDate;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getSecuCode() {
        return secuCode;
    }

    public void setSecuCode(String secuCode) {
        this.secuCode = secuCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Double getNetBuyAmt() {
        return netBuyAmt;
    }

    public void setNetBuyAmt(Double netBuyAmt) {
        this.netBuyAmt = netBuyAmt;
    }

    public Double getBuyAmt() {
        return buyAmt;
    }

    public void setBuyAmt(Double buyAmt) {
        this.buyAmt = buyAmt;
    }

    public Double getSellAmt() {
        return sellAmt;
    }

    public void setSellAmt(Double sellAmt) {
        this.sellAmt = sellAmt;
    }

    public Double getTotalAmt() {
        return totalAmt;
    }

    public void setTotalAmt(Double totalAmt) {
        this.totalAmt = totalAmt;
    }

    public Double getTurnoverRate() {
        return turnoverRate;
    }

    public void setTurnoverRate(Double turnoverRate) {
        this.turnoverRate = turnoverRate;
    }

    public Double getFreeCap() {
        return freeCap;
    }

    public void setFreeCap(Double freeCap) {
        this.freeCap = freeCap;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReasonDetail() {
        return reasonDetail;
    }

    public void setReasonDetail(String reasonDetail) {
        this.reasonDetail = reasonDetail;
    }

    public Double getAccumAmount() {
        return accumAmount;
    }

    public void setAccumAmount(Double accumAmount) {
        this.accumAmount = accumAmount;
    }

    public Double getDealRatio() {
        return dealRatio;
    }

    public void setDealRatio(Double dealRatio) {
        this.dealRatio = dealRatio;
    }

    public Double getNetRatio() {
        return netRatio;
    }

    public void setNetRatio(Double netRatio) {
        this.netRatio = netRatio;
    }

    public Double getD1Change() {
        return d1Change;
    }

    public void setD1Change(Double d1Change) {
        this.d1Change = d1Change;
    }

    public Double getD2Change() {
        return d2Change;
    }

    public void setD2Change(Double d2Change) {
        this.d2Change = d2Change;
    }

    public Double getD5Change() {
        return d5Change;
    }

    public void setD5Change(Double d5Change) {
        this.d5Change = d5Change;
    }

    public Double getD10Change() {
        return d10Change;
    }

    public void setD10Change(Double d10Change) {
        this.d10Change = d10Change;
    }

    @Override
    public String toString() {
        return "LongHuBangItem{" +
                "tradeDate='" + tradeDate + '\'' +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", netBuyAmt=" + netBuyAmt +
                ", reason='" + reason + '\'' +
                '}';
    }
}
