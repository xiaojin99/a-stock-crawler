package com.stock.crawler.calendar;

/**
 * 交易日历数据源异常。
 */
public class TradingCalendarSourceException extends RuntimeException {

    private final TradingCalendarFailureReason reason;

    public TradingCalendarSourceException(
            TradingCalendarFailureReason reason,
            String message) {
        super(message);
        this.reason = reason;
    }

    public TradingCalendarSourceException(
            TradingCalendarFailureReason reason,
            String message,
            Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public TradingCalendarFailureReason reason() {
        return reason;
    }
}
