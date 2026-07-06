package com.stock.crawler.model;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 个股资金流向点位。
 */
public class FundFlowPoint implements Serializable {

    private static final long serialVersionUID = 1L;

    private String time;
    private BigDecimal mainNet;
    private BigDecimal smallNet;
    private BigDecimal midNet;
    private BigDecimal largeNet;
    private BigDecimal superNet;

    public FundFlowPoint() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public BigDecimal getMainNet() { return mainNet; }
    public void setMainNet(BigDecimal mainNet) { this.mainNet = mainNet; }
    public BigDecimal getSmallNet() { return smallNet; }
    public void setSmallNet(BigDecimal smallNet) { this.smallNet = smallNet; }
    public BigDecimal getMidNet() { return midNet; }
    public void setMidNet(BigDecimal midNet) { this.midNet = midNet; }
    public BigDecimal getLargeNet() { return largeNet; }
    public void setLargeNet(BigDecimal largeNet) { this.largeNet = largeNet; }
    public BigDecimal getSuperNet() { return superNet; }
    public void setSuperNet(BigDecimal superNet) { this.superNet = superNet; }

    public static class Builder {
        private final FundFlowPoint instance = new FundFlowPoint();

        public Builder time(String time) { instance.time = time; return this; }
        public Builder mainNet(BigDecimal mainNet) { instance.mainNet = mainNet; return this; }
        public Builder smallNet(BigDecimal smallNet) { instance.smallNet = smallNet; return this; }
        public Builder midNet(BigDecimal midNet) { instance.midNet = midNet; return this; }
        public Builder largeNet(BigDecimal largeNet) { instance.largeNet = largeNet; return this; }
        public Builder superNet(BigDecimal superNet) { instance.superNet = superNet; return this; }
        public FundFlowPoint build() { return instance; }
    }
}
