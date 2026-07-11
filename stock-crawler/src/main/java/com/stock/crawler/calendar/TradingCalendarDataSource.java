package com.stock.crawler.calendar;

import com.stock.crawler.model.TradingCalendarSnapshot;

import java.time.YearMonth;

/**
 * 交易日历数据源。
 */
public interface TradingCalendarDataSource {

    String name();

    TradingCalendarSnapshot fetch(YearMonth month);
}
