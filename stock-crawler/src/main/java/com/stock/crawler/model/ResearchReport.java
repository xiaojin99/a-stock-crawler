package com.stock.crawler.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 研报数据结构
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResearchReport {

    private String title;

    @JsonProperty("stockName")
    private String stockName;

    @JsonProperty("stockCode")
    private String stockCode;

    @JsonProperty("orgSName")
    private String orgSName;

    @JsonProperty("publishDate")
    private String publishDate;

    @JsonProperty("predictThisYearEps")
    private String predictThisYearEps;

    @JsonProperty("predictThisYearPe")
    private String predictThisYearPe;

    @JsonProperty("emRatingName")
    private String emRatingName;

    private String researcher;

    @JsonProperty("infoCode")
    private String infoCode;

    @JsonProperty("indvInduName")
    private String industry;

    @JsonProperty("encodeUrl")
    private String encodeUrl;

    public ResearchReport() {
    }

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public String getStockCode() {
        return stockCode;
    }

    public void setStockCode(String stockCode) {
        this.stockCode = stockCode;
    }

    public String getOrgSName() {
        return orgSName;
    }

    public void setOrgSName(String orgSName) {
        this.orgSName = orgSName;
    }

    public String getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(String publishDate) {
        this.publishDate = publishDate;
    }

    public String getPredictThisYearEps() {
        return predictThisYearEps;
    }

    public void setPredictThisYearEps(String predictThisYearEps) {
        this.predictThisYearEps = predictThisYearEps;
    }

    public String getPredictThisYearPe() {
        return predictThisYearPe;
    }

    public void setPredictThisYearPe(String predictThisYearPe) {
        this.predictThisYearPe = predictThisYearPe;
    }

    public String getEmRatingName() {
        return emRatingName;
    }

    public void setEmRatingName(String emRatingName) {
        this.emRatingName = emRatingName;
    }

    public String getResearcher() {
        return researcher;
    }

    public void setResearcher(String researcher) {
        this.researcher = researcher;
    }

    public String getInfoCode() {
        return infoCode;
    }

    public void setInfoCode(String infoCode) {
        this.infoCode = infoCode;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getEncodeUrl() {
        return encodeUrl;
    }

    public void setEncodeUrl(String encodeUrl) {
        this.encodeUrl = encodeUrl;
    }

    @Override
    public String toString() {
        return "ResearchReport{" +
                "title='" + title + '\'' +
                ", stockName='" + stockName + '\'' +
                ", stockCode='" + stockCode + '\'' +
                ", orgSName='" + orgSName + '\'' +
                ", publishDate='" + publishDate + '\'' +
                ", emRatingName='" + emRatingName + '\'' +
                '}';
    }
}
