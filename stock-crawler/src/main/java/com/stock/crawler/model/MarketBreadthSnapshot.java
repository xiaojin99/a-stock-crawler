package com.stock.crawler.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * A 股市场广度快照。
 */
public class MarketBreadthSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int schemaVersion;
    private final Instant asOf;
    private final int riseCount;
    private final int fallCount;
    private final int flatCount;
    private final int suspendedCount;
    private final int limitUpCount;
    private final int limitDownCount;
    private final int strongUpCount;
    private final int strongDownCount;
    /**
     * 全市场精确平均涨跌幅。聚合分布接口无法提供该值时为 {@code null}，
     * 调用方不得使用区间中点估算。
     */
    private final BigDecimal averageChangePercent;

    public MarketBreadthSnapshot(
            int schemaVersion,
            Instant asOf,
            int riseCount,
            int fallCount,
            int flatCount,
            int suspendedCount,
            int limitUpCount,
            int limitDownCount,
            int strongUpCount,
            int strongDownCount,
            BigDecimal averageChangePercent) {
        this.schemaVersion = schemaVersion;
        this.asOf = asOf;
        this.riseCount = riseCount;
        this.fallCount = fallCount;
        this.flatCount = flatCount;
        this.suspendedCount = suspendedCount;
        this.limitUpCount = limitUpCount;
        this.limitDownCount = limitDownCount;
        this.strongUpCount = strongUpCount;
        this.strongDownCount = strongDownCount;
        this.averageChangePercent = averageChangePercent;
    }

    public int getSchemaVersion() { return schemaVersion; }
    public Instant getAsOf() { return asOf; }
    public int getRiseCount() { return riseCount; }
    public int getFallCount() { return fallCount; }
    public int getFlatCount() { return flatCount; }
    public int getSuspendedCount() { return suspendedCount; }
    public int getLimitUpCount() { return limitUpCount; }
    public int getLimitDownCount() { return limitDownCount; }
    public int getStrongUpCount() { return strongUpCount; }
    public int getStrongDownCount() { return strongDownCount; }
    public BigDecimal getAverageChangePercent() { return averageChangePercent; }

    public int getActiveCount() {
        return riseCount + fallCount + flatCount;
    }
}
