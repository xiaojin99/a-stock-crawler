package com.stock.crawler.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 技术指标
 */
public class TechnicalIndicators implements Serializable {

    private static final long serialVersionUID = 1L;

    private String code;
    private LocalDate date;

    // 均线系统
    private BigDecimal ma5;
    private BigDecimal ma10;
    private BigDecimal ma20;
    private BigDecimal ma60;

    // MACD
    private BigDecimal macdDif;
    private BigDecimal macdDea;
    private BigDecimal macdHistogram;

    // RSI
    private BigDecimal rsi6;
    private BigDecimal rsi12;
    private BigDecimal rsi24;

    // KDJ
    private BigDecimal kdjK;
    private BigDecimal kdjD;
    private BigDecimal kdjJ;

    // 布林带
    private BigDecimal bollUpper;
    private BigDecimal bollMiddle;
    private BigDecimal bollLower;

    // 成交量均线
    private BigDecimal volumeMa5;
    private BigDecimal volumeMa10;

    // 信号
    private String trend;
    private String macdSignal;
    private String rsiSignal;

    public TechnicalIndicators() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public BigDecimal getMa5() { return ma5; }
    public void setMa5(BigDecimal ma5) { this.ma5 = ma5; }
    public BigDecimal getMa10() { return ma10; }
    public void setMa10(BigDecimal ma10) { this.ma10 = ma10; }
    public BigDecimal getMa20() { return ma20; }
    public void setMa20(BigDecimal ma20) { this.ma20 = ma20; }
    public BigDecimal getMa60() { return ma60; }
    public void setMa60(BigDecimal ma60) { this.ma60 = ma60; }
    public BigDecimal getMacdDif() { return macdDif; }
    public void setMacdDif(BigDecimal macdDif) { this.macdDif = macdDif; }
    public BigDecimal getMacdDea() { return macdDea; }
    public void setMacdDea(BigDecimal macdDea) { this.macdDea = macdDea; }
    public BigDecimal getMacdHistogram() { return macdHistogram; }
    public void setMacdHistogram(BigDecimal macdHistogram) { this.macdHistogram = macdHistogram; }
    public BigDecimal getRsi6() { return rsi6; }
    public void setRsi6(BigDecimal rsi6) { this.rsi6 = rsi6; }
    public BigDecimal getRsi12() { return rsi12; }
    public void setRsi12(BigDecimal rsi12) { this.rsi12 = rsi12; }
    public BigDecimal getRsi24() { return rsi24; }
    public void setRsi24(BigDecimal rsi24) { this.rsi24 = rsi24; }
    public BigDecimal getKdjK() { return kdjK; }
    public void setKdjK(BigDecimal kdjK) { this.kdjK = kdjK; }
    public BigDecimal getKdjD() { return kdjD; }
    public void setKdjD(BigDecimal kdjD) { this.kdjD = kdjD; }
    public BigDecimal getKdjJ() { return kdjJ; }
    public void setKdjJ(BigDecimal kdjJ) { this.kdjJ = kdjJ; }
    public BigDecimal getBollUpper() { return bollUpper; }
    public void setBollUpper(BigDecimal bollUpper) { this.bollUpper = bollUpper; }
    public BigDecimal getBollMiddle() { return bollMiddle; }
    public void setBollMiddle(BigDecimal bollMiddle) { this.bollMiddle = bollMiddle; }
    public BigDecimal getBollLower() { return bollLower; }
    public void setBollLower(BigDecimal bollLower) { this.bollLower = bollLower; }
    public BigDecimal getVolumeMa5() { return volumeMa5; }
    public void setVolumeMa5(BigDecimal volumeMa5) { this.volumeMa5 = volumeMa5; }
    public BigDecimal getVolumeMa10() { return volumeMa10; }
    public void setVolumeMa10(BigDecimal volumeMa10) { this.volumeMa10 = volumeMa10; }
    public String getTrend() { return trend; }
    public void setTrend(String trend) { this.trend = trend; }
    public String getMacdSignal() { return macdSignal; }
    public void setMacdSignal(String macdSignal) { this.macdSignal = macdSignal; }
    public String getRsiSignal() { return rsiSignal; }
    public void setRsiSignal(String rsiSignal) { this.rsiSignal = rsiSignal; }

    public static class Builder {
        private final TechnicalIndicators instance = new TechnicalIndicators();

        public Builder code(String code) { instance.code = code; return this; }
        public Builder date(LocalDate date) { instance.date = date; return this; }
        public Builder ma5(BigDecimal ma5) { instance.ma5 = ma5; return this; }
        public Builder ma10(BigDecimal ma10) { instance.ma10 = ma10; return this; }
        public Builder ma20(BigDecimal ma20) { instance.ma20 = ma20; return this; }
        public Builder ma60(BigDecimal ma60) { instance.ma60 = ma60; return this; }
        public Builder macdDif(BigDecimal macdDif) { instance.macdDif = macdDif; return this; }
        public Builder macdDea(BigDecimal macdDea) { instance.macdDea = macdDea; return this; }
        public Builder macdHistogram(BigDecimal macdHistogram) { instance.macdHistogram = macdHistogram; return this; }
        public Builder rsi6(BigDecimal rsi6) { instance.rsi6 = rsi6; return this; }
        public Builder rsi12(BigDecimal rsi12) { instance.rsi12 = rsi12; return this; }
        public Builder rsi24(BigDecimal rsi24) { instance.rsi24 = rsi24; return this; }
        public Builder kdjK(BigDecimal kdjK) { instance.kdjK = kdjK; return this; }
        public Builder kdjD(BigDecimal kdjD) { instance.kdjD = kdjD; return this; }
        public Builder kdjJ(BigDecimal kdjJ) { instance.kdjJ = kdjJ; return this; }
        public Builder bollUpper(BigDecimal bollUpper) { instance.bollUpper = bollUpper; return this; }
        public Builder bollMiddle(BigDecimal bollMiddle) { instance.bollMiddle = bollMiddle; return this; }
        public Builder bollLower(BigDecimal bollLower) { instance.bollLower = bollLower; return this; }
        public Builder volumeMa5(BigDecimal volumeMa5) { instance.volumeMa5 = volumeMa5; return this; }
        public Builder volumeMa10(BigDecimal volumeMa10) { instance.volumeMa10 = volumeMa10; return this; }
        public TechnicalIndicators build() { return instance; }
    }

    @Override
    public String toString() {
        return "TechnicalIndicators{code='" + code + "', date=" + date + ", trend='" + trend + "'}";
    }
}
