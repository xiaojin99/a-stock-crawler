package com.stock.crawler.service;

import com.stock.crawler.calendar.EastMoneyTradingCalendarDataSource;
import com.stock.crawler.calendar.SzseTradingCalendarDataSource;
import com.stock.crawler.calendar.TradingCalendarDataSource;
import com.stock.crawler.calendar.TradingCalendarSourceException;
import com.stock.crawler.model.DataResult;
import com.stock.crawler.model.TradingCalendarSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.YearMonth;

/**
 * 交易日历主备数据源编排服务。
 */
public class TradingCalendarService {

    private static final Logger log = LoggerFactory.getLogger(TradingCalendarService.class);

    private final TradingCalendarDataSource primarySource;
    private final TradingCalendarDataSource fallbackSource;

    public TradingCalendarService() {
        this(new SzseTradingCalendarDataSource(), new EastMoneyTradingCalendarDataSource());
    }

    public TradingCalendarService(
            TradingCalendarDataSource primarySource,
            TradingCalendarDataSource fallbackSource) {
        this.primarySource = primarySource;
        this.fallbackSource = fallbackSource;
    }

    public DataResult<TradingCalendarSnapshot> getTradingCalendar(YearMonth month) {
        if (month == null) {
            throw new IllegalArgumentException("month must not be null");
        }
        long startedAt = System.nanoTime();
        String primaryFailure;
        try {
            TradingCalendarSnapshot snapshot = primarySource.fetch(month);
            return DataResult.success(
                    snapshot, "calendar:" + primarySource.name(), elapsedMs(startedAt));
        } catch (TradingCalendarSourceException ex) {
            primaryFailure = ex.reason().name();
            log.warn("trading_calendar_primary_failed source={} month={} reason={} message={}",
                    primarySource.name(), month, ex.reason(), ex.getMessage());
        } catch (RuntimeException ex) {
            primaryFailure = unexpectedReason(ex);
            log.warn("trading_calendar_primary_failed source={} month={} reason={} message={}",
                    primarySource.name(), month, primaryFailure, ex.getMessage(), ex);
        }

        try {
            TradingCalendarSnapshot snapshot = fallbackSource.fetch(month);
            DataResult<TradingCalendarSnapshot> result = DataResult.success(
                    snapshot,
                    "calendar:" + fallbackSource.name() + "-fallback",
                    elapsedMs(startedAt));
            result.setMessage("Primary source failed: " + primaryFailure);
            return result;
        } catch (TradingCalendarSourceException fallbackFailure) {
            log.warn("trading_calendar_fallback_failed source={} month={} reason={} message={}",
                    fallbackSource.name(), month, fallbackFailure.reason(), fallbackFailure.getMessage());
            return DataResult.failure(
                    "calendar:" + primarySource.name() + "-" + fallbackSource.name(),
                    "Primary=" + primaryFailure
                            + ", fallback=" + fallbackFailure.reason(),
                    elapsedMs(startedAt));
        } catch (RuntimeException fallbackFailure) {
            String fallbackReason = unexpectedReason(fallbackFailure);
            log.warn("trading_calendar_fallback_failed source={} month={} reason={} message={}",
                    fallbackSource.name(), month, fallbackReason,
                    fallbackFailure.getMessage(), fallbackFailure);
            return DataResult.failure(
                    "calendar:" + primarySource.name() + "-" + fallbackSource.name(),
                    "Primary=" + primaryFailure + ", fallback=" + fallbackReason,
                    elapsedMs(startedAt));
        }
    }

    private String unexpectedReason(RuntimeException exception) {
        return "UNEXPECTED_" + exception.getClass().getSimpleName();
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
