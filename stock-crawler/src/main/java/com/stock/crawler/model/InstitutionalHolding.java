package com.stock.crawler.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 机构持仓数据
 * 数据来源：东方财富 RPT_F10_EH_HOLDERNUM / RPT_F10_EH_HOLDDETAIL
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstitutionalHolding {

    private String securityCode;
    private String securityNameAbbr;
    private String reportDate;
    private String reportDateName;

    /** 机构家数 */
    private Integer holderNum;

    /** 机构合计持股数（股） */
    private Double holdTotalShare;

    /** 机构合计持股市值（元） */
    private Double holdTotalMarketCap;

    /** 占流通股比例（%） */
    private Double holdTotalRatio;

    /** 较上期增减股数 */
    private Double holdChangeShare;

    /** 基金持股家数 */
    private Integer fundHolderNum;

    /** 基金持股数（股） */
    private Double fundHoldShare;

    /** 基金占流通股比（%） */
    private Double fundHoldRatio;

    /** QFII 持股家数 */
    private Integer qfiiHolderNum;

    /** 社保持股家数 */
    private Integer socialSecurityHolderNum;

    /** 保险持股家数 */
    private Integer insuranceHolderNum;

    public InstitutionalHolding() {
    }

    public String getSecurityCode() { return securityCode; }
    public void setSecurityCode(String securityCode) { this.securityCode = securityCode; }

    public String getSecurityNameAbbr() { return securityNameAbbr; }
    public void setSecurityNameAbbr(String securityNameAbbr) { this.securityNameAbbr = securityNameAbbr; }

    public String getReportDate() { return reportDate; }
    public void setReportDate(String reportDate) { this.reportDate = reportDate; }

    public String getReportDateName() { return reportDateName; }
    public void setReportDateName(String reportDateName) { this.reportDateName = reportDateName; }

    public Integer getHolderNum() { return holderNum; }
    public void setHolderNum(Integer holderNum) { this.holderNum = holderNum; }

    public Double getHoldTotalShare() { return holdTotalShare; }
    public void setHoldTotalShare(Double holdTotalShare) { this.holdTotalShare = holdTotalShare; }

    public Double getHoldTotalMarketCap() { return holdTotalMarketCap; }
    public void setHoldTotalMarketCap(Double holdTotalMarketCap) { this.holdTotalMarketCap = holdTotalMarketCap; }

    public Double getHoldTotalRatio() { return holdTotalRatio; }
    public void setHoldTotalRatio(Double holdTotalRatio) { this.holdTotalRatio = holdTotalRatio; }

    public Double getHoldChangeShare() { return holdChangeShare; }
    public void setHoldChangeShare(Double holdChangeShare) { this.holdChangeShare = holdChangeShare; }

    public Integer getFundHolderNum() { return fundHolderNum; }
    public void setFundHolderNum(Integer fundHolderNum) { this.fundHolderNum = fundHolderNum; }

    public Double getFundHoldShare() { return fundHoldShare; }
    public void setFundHoldShare(Double fundHoldShare) { this.fundHoldShare = fundHoldShare; }

    public Double getFundHoldRatio() { return fundHoldRatio; }
    public void setFundHoldRatio(Double fundHoldRatio) { this.fundHoldRatio = fundHoldRatio; }

    public Integer getQfiiHolderNum() { return qfiiHolderNum; }
    public void setQfiiHolderNum(Integer qfiiHolderNum) { this.qfiiHolderNum = qfiiHolderNum; }

    public Integer getSocialSecurityHolderNum() { return socialSecurityHolderNum; }
    public void setSocialSecurityHolderNum(Integer socialSecurityHolderNum) { this.socialSecurityHolderNum = socialSecurityHolderNum; }

    public Integer getInsuranceHolderNum() { return insuranceHolderNum; }
    public void setInsuranceHolderNum(Integer insuranceHolderNum) { this.insuranceHolderNum = insuranceHolderNum; }

    @Override
    public String toString() {
        return "InstitutionalHolding{" +
                "securityCode='" + securityCode + '\'' +
                ", reportDateName='" + reportDateName + '\'' +
                ", holderNum=" + holderNum +
                ", holdTotalRatio=" + holdTotalRatio +
                '}';
    }
}
