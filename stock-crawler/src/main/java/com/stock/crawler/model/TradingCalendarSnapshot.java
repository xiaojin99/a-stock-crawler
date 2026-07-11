package com.stock.crawler.model;

import java.io.Serializable;
import java.time.YearMonth;
import java.util.List;

/**
 * 单月交易日历快照。
 */
public record TradingCalendarSnapshot(
        YearMonth month,
        List<TradingCalendarDay> days
) implements Serializable {

    public TradingCalendarSnapshot {
        days = days == null ? List.of() : List.copyOf(days);
    }
}
