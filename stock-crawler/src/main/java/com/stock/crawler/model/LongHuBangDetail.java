package com.stock.crawler.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 龙虎榜营业部明细数据结构
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LongHuBangDetail {

    private Integer rank;
    private String operName;
    private Double buyAmt;
    private Double buyPercent;
    private Double sellAmt;
    private Double sellPercent;
    private Double netAmt;
    private String direction;

    public LongHuBangDetail() {
    }

    // Getters and Setters
    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public String getOperName() {
        return operName;
    }

    public void setOperName(String operName) {
        this.operName = operName;
    }

    public Double getBuyAmt() {
        return buyAmt;
    }

    public void setBuyAmt(Double buyAmt) {
        this.buyAmt = buyAmt;
    }

    public Double getBuyPercent() {
        return buyPercent;
    }

    public void setBuyPercent(Double buyPercent) {
        this.buyPercent = buyPercent;
    }

    public Double getSellAmt() {
        return sellAmt;
    }

    public void setSellAmt(Double sellAmt) {
        this.sellAmt = sellAmt;
    }

    public Double getSellPercent() {
        return sellPercent;
    }

    public void setSellPercent(Double sellPercent) {
        this.sellPercent = sellPercent;
    }

    public Double getNetAmt() {
        return netAmt;
    }

    public void setNetAmt(Double netAmt) {
        this.netAmt = netAmt;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    @Override
    public String toString() {
        return "LongHuBangDetail{" +
                "rank=" + rank +
                ", operName='" + operName + '\'' +
                ", buyAmt=" + buyAmt +
                ", sellAmt=" + sellAmt +
                ", netAmt=" + netAmt +
                ", direction='" + direction + '\'' +
                '}';
    }
}
