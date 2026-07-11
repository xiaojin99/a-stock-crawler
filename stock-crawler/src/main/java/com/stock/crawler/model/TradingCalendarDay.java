package com.stock.crawler.model;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 单个自然日的交易状态。
 */
public record TradingCalendarDay(
        LocalDate date,
        TradingDayStatus status,
        String reason
) implements Serializable {
}
