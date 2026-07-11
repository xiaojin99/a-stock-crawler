package com.stock.crawler.calendar;

/**
 * 交易日历数据源失败分类。
 */
public enum TradingCalendarFailureReason {
    NETWORK,
    INVALID_RESPONSE,
    INCOMPLETE_COVERAGE,
    UNSUPPORTED_SESSION
}
