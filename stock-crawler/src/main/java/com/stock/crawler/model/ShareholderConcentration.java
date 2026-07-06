package com.stock.crawler.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 股东集中度数据
 * 数据来源：东方财富 RPT_F10_EH_HOLDERNUM
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShareholderConcentration {

    private String securityCode;
    private String securityNameAbbr;
    private String endDate;
    private String noticeDate;

    /** 股东总户数 */
    private Integer holderTotalNum;

    /** 较上期增减比例（%） */
    private Double totalNumRatio;

    /** 户均持股数量（股） */
    private Double avgFreeShares;

    /** 户均持股金额（元） */
    private Double avgHoldAmt;

    /** 筹码集中度（如"非常分散"/"较分散"/"适中"/"较集中"/"非常集中"） */
    private String holdFocus;

    /** 流通股持股比例合计（%） */
    private Double holdRatioTotal;

    /** 股东人数变化 */
    private Integer holderTotalNumChange;

    public ShareholderConcentration() {
    }

    public String getSecurityCode() { return securityCode; }
    public void setSecurityCode(String securityCode) { this.securityCode = securityCode; }

    public String getSecurityNameAbbr() { return securityNameAbbr; }
    public void setSecurityNameAbbr(String securityNameAbbr) { this.securityNameAbbr = securityNameAbbr; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getNoticeDate() { return noticeDate; }
    public void setNoticeDate(String noticeDate) { this.noticeDate = noticeDate; }

    public Integer getHolderTotalNum() { return holderTotalNum; }
    public void setHolderTotalNum(Integer holderTotalNum) { this.holderTotalNum = holderTotalNum; }

    public Double getTotalNumRatio() { return totalNumRatio; }
    public void setTotalNumRatio(Double totalNumRatio) { this.totalNumRatio = totalNumRatio; }

    public Double getAvgFreeShares() { return avgFreeShares; }
    public void setAvgFreeShares(Double avgFreeShares) { this.avgFreeShares = avgFreeShares; }

    public Double getAvgHoldAmt() { return avgHoldAmt; }
    public void setAvgHoldAmt(Double avgHoldAmt) { this.avgHoldAmt = avgHoldAmt; }

    public String getHoldFocus() { return holdFocus; }
    public void setHoldFocus(String holdFocus) { this.holdFocus = holdFocus; }

    public Double getHoldRatioTotal() { return holdRatioTotal; }
    public void setHoldRatioTotal(Double holdRatioTotal) { this.holdRatioTotal = holdRatioTotal; }

    public Integer getHolderTotalNumChange() { return holderTotalNumChange; }
    public void setHolderTotalNumChange(Integer holderTotalNumChange) { this.holderTotalNumChange = holderTotalNumChange; }

    @Override
    public String toString() {
        return "ShareholderConcentration{" +
                "securityCode='" + securityCode + '\'' +
                ", endDate='" + endDate + '\'' +
                ", holderTotalNum=" + holderTotalNum +
                ", holdFocus='" + holdFocus + '\'' +
                '}';
    }
}
