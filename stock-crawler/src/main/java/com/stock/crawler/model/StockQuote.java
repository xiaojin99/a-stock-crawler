package com.stock.crawler.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 股票实时行情
 */
public class StockQuote implements Serializable {

    private static final long serialVersionUID = 1L;

    private String code;
    private String name;
    private BigDecimal price;
    private BigDecimal preClose;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private Long volume;
    private BigDecimal amount;
    private BigDecimal change;
    private BigDecimal changePercent;
    private BigDecimal turnoverRate;
    private BigDecimal pe;
    private BigDecimal pb;
    private BigDecimal marketCap;
    private BigDecimal bid1Price;
    private Long bid1Volume;
    private BigDecimal ask1Price;
    private Long ask1Volume;
    private LocalDateTime time;
    private String source;

    public StockQuote() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getPreClose() { return preClose; }
    public void setPreClose(BigDecimal preClose) { this.preClose = preClose; }
    public BigDecimal getOpen() { return open; }
    public void setOpen(BigDecimal open) { this.open = open; }
    public BigDecimal getHigh() { return high; }
    public void setHigh(BigDecimal high) { this.high = high; }
    public BigDecimal getLow() { return low; }
    public void setLow(BigDecimal low) { this.low = low; }
    public Long getVolume() { return volume; }
    public void setVolume(Long volume) { this.volume = volume; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getChange() { return change; }
    public void setChange(BigDecimal change) { this.change = change; }
    public BigDecimal getChangePercent() { return changePercent; }
    public void setChangePercent(BigDecimal changePercent) { this.changePercent = changePercent; }
    public BigDecimal getTurnoverRate() { return turnoverRate; }
    public void setTurnoverRate(BigDecimal turnoverRate) { this.turnoverRate = turnoverRate; }
    public BigDecimal getPe() { return pe; }
    public void setPe(BigDecimal pe) { this.pe = pe; }
    public BigDecimal getPb() { return pb; }
    public void setPb(BigDecimal pb) { this.pb = pb; }
    public BigDecimal getMarketCap() { return marketCap; }
    public void setMarketCap(BigDecimal marketCap) { this.marketCap = marketCap; }
    public BigDecimal getBid1Price() { return bid1Price; }
    public void setBid1Price(BigDecimal bid1Price) { this.bid1Price = bid1Price; }
    public Long getBid1Volume() { return bid1Volume; }
    public void setBid1Volume(Long bid1Volume) { this.bid1Volume = bid1Volume; }
    public BigDecimal getAsk1Price() { return ask1Price; }
    public void setAsk1Price(BigDecimal ask1Price) { this.ask1Price = ask1Price; }
    public Long getAsk1Volume() { return ask1Volume; }
    public void setAsk1Volume(Long ask1Volume) { this.ask1Volume = ask1Volume; }
    public LocalDateTime getTime() { return time; }
    public void setTime(LocalDateTime time) { this.time = time; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public static class Builder {
        private final StockQuote instance = new StockQuote();

        public Builder code(String code) { instance.code = code; return this; }
        public Builder name(String name) { instance.name = name; return this; }
        public Builder price(BigDecimal price) { instance.price = price; return this; }
        public Builder preClose(BigDecimal preClose) { instance.preClose = preClose; return this; }
        public Builder open(BigDecimal open) { instance.open = open; return this; }
        public Builder high(BigDecimal high) { instance.high = high; return this; }
        public Builder low(BigDecimal low) { instance.low = low; return this; }
        public Builder volume(Long volume) { instance.volume = volume; return this; }
        public Builder amount(BigDecimal amount) { instance.amount = amount; return this; }
        public Builder change(BigDecimal change) { instance.change = change; return this; }
        public Builder changePercent(BigDecimal changePercent) { instance.changePercent = changePercent; return this; }
        public Builder turnoverRate(BigDecimal turnoverRate) { instance.turnoverRate = turnoverRate; return this; }
        public Builder pe(BigDecimal pe) { instance.pe = pe; return this; }
        public Builder pb(BigDecimal pb) { instance.pb = pb; return this; }
        public Builder marketCap(BigDecimal marketCap) { instance.marketCap = marketCap; return this; }
        public Builder bid1Price(BigDecimal bid1Price) { instance.bid1Price = bid1Price; return this; }
        public Builder bid1Volume(Long bid1Volume) { instance.bid1Volume = bid1Volume; return this; }
        public Builder ask1Price(BigDecimal ask1Price) { instance.ask1Price = ask1Price; return this; }
        public Builder ask1Volume(Long ask1Volume) { instance.ask1Volume = ask1Volume; return this; }
        public Builder time(LocalDateTime time) { instance.time = time; return this; }
        public Builder source(String source) { instance.source = source; return this; }
        public StockQuote build() { return instance; }
    }

    @Override
    public String toString() {
        return "StockQuote{code='" + code + "', name='" + name + "', price=" + price + ", source='" + source + "'}";
    }
}
