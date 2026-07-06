package com.stock.crawler.model;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 个股基础资料。
 */
public class StockBasicInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String code;
    private String name;
    private String industry;
    private BigDecimal totalShares;
    private BigDecimal floatShares;
    private BigDecimal marketCap;
    private BigDecimal floatMarketCap;
    private String listDate;
    private BigDecimal price;

    public StockBasicInfo() {
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }
    public BigDecimal getTotalShares() { return totalShares; }
    public void setTotalShares(BigDecimal totalShares) { this.totalShares = totalShares; }
    public BigDecimal getFloatShares() { return floatShares; }
    public void setFloatShares(BigDecimal floatShares) { this.floatShares = floatShares; }
    public BigDecimal getMarketCap() { return marketCap; }
    public void setMarketCap(BigDecimal marketCap) { this.marketCap = marketCap; }
    public BigDecimal getFloatMarketCap() { return floatMarketCap; }
    public void setFloatMarketCap(BigDecimal floatMarketCap) { this.floatMarketCap = floatMarketCap; }
    public String getListDate() { return listDate; }
    public void setListDate(String listDate) { this.listDate = listDate; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
