package com.stock.crawler.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * K线数据
 */
public class KLineData implements Serializable {

    private static final long serialVersionUID = 1L;

    private String code;
    private LocalDate date;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private Long volume;
    private BigDecimal amount;
    private BigDecimal changePercent;
    private BigDecimal turnoverRate;
    private String period;

    public KLineData() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public BigDecimal getOpen() { return open; }
    public void setOpen(BigDecimal open) { this.open = open; }
    public BigDecimal getHigh() { return high; }
    public void setHigh(BigDecimal high) { this.high = high; }
    public BigDecimal getLow() { return low; }
    public void setLow(BigDecimal low) { this.low = low; }
    public BigDecimal getClose() { return close; }
    public void setClose(BigDecimal close) { this.close = close; }
    public Long getVolume() { return volume; }
    public void setVolume(Long volume) { this.volume = volume; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getChangePercent() { return changePercent; }
    public void setChangePercent(BigDecimal changePercent) { this.changePercent = changePercent; }
    public BigDecimal getTurnoverRate() { return turnoverRate; }
    public void setTurnoverRate(BigDecimal turnoverRate) { this.turnoverRate = turnoverRate; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public static class Builder {
        private final KLineData instance = new KLineData();

        public Builder code(String code) { instance.code = code; return this; }
        public Builder date(LocalDate date) { instance.date = date; return this; }
        public Builder open(BigDecimal open) { instance.open = open; return this; }
        public Builder high(BigDecimal high) { instance.high = high; return this; }
        public Builder low(BigDecimal low) { instance.low = low; return this; }
        public Builder close(BigDecimal close) { instance.close = close; return this; }
        public Builder volume(Long volume) { instance.volume = volume; return this; }
        public Builder amount(BigDecimal amount) { instance.amount = amount; return this; }
        public Builder changePercent(BigDecimal changePercent) { instance.changePercent = changePercent; return this; }
        public Builder turnoverRate(BigDecimal turnoverRate) { instance.turnoverRate = turnoverRate; return this; }
        public Builder period(String period) { instance.period = period; return this; }
        public KLineData build() { return instance; }
    }

    @Override
    public String toString() {
        return "KLineData{code='" + code + "', date=" + date + ", close=" + close + ", period='" + period + "'}";
    }
}
