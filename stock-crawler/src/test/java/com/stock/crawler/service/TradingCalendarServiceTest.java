package com.stock.crawler.service;

import com.stock.crawler.calendar.TradingCalendarDataSource;
import com.stock.crawler.calendar.TradingCalendarFailureReason;
import com.stock.crawler.calendar.TradingCalendarSourceException;
import com.stock.crawler.model.DataResult;
import com.stock.crawler.model.TradingCalendarDay;
import com.stock.crawler.model.TradingCalendarSnapshot;
import com.stock.crawler.model.TradingDayStatus;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradingCalendarServiceTest {

    private static final YearMonth JULY_2026 = YearMonth.of(2026, 7);

    @Test
    void usesThePrimarySourceWhenItSucceeds() {
        TradingCalendarService service = new TradingCalendarService(
                source("szse", snapshot()),
                failingSource("eastmoney"));

        DataResult<TradingCalendarSnapshot> result = service.getTradingCalendar(JULY_2026);

        assertTrue(result.isSuccess());
        assertEquals("calendar:szse", result.getSource());
    }

    @Test
    void fallsBackToEastMoneyAfterThePrimaryFails() {
        TradingCalendarService service = new TradingCalendarService(
                failingSource("szse"),
                source("eastmoney", snapshot()));

        DataResult<TradingCalendarSnapshot> result = service.getTradingCalendar(JULY_2026);

        assertTrue(result.isSuccess());
        assertEquals("calendar:eastmoney-fallback", result.getSource());
    }

    @Test
    void returnsFailureWhenBothSourcesFail() {
        TradingCalendarService service = new TradingCalendarService(
                failingSource("szse"),
                failingSource("eastmoney"));

        DataResult<TradingCalendarSnapshot> result = service.getTradingCalendar(JULY_2026);

        assertFalse(result.isSuccess());
        assertEquals("calendar:szse-eastmoney", result.getSource());
    }

    @Test
    void fallsBackAfterAnUnexpectedPrimaryFailure() {
        TradingCalendarService service = new TradingCalendarService(
                unexpectedFailingSource("szse"),
                source("eastmoney", snapshot()));

        DataResult<TradingCalendarSnapshot> result = service.getTradingCalendar(JULY_2026);

        assertTrue(result.isSuccess());
        assertEquals("calendar:eastmoney-fallback", result.getSource());
    }

    @Test
    void returnsFailureAfterAnUnexpectedFallbackFailure() {
        TradingCalendarService service = new TradingCalendarService(
                failingSource("szse"),
                unexpectedFailingSource("eastmoney"));

        DataResult<TradingCalendarSnapshot> result = service.getTradingCalendar(JULY_2026);

        assertFalse(result.isSuccess());
        assertEquals("calendar:szse-eastmoney", result.getSource());
    }

    private TradingCalendarDataSource source(String name, TradingCalendarSnapshot snapshot) {
        return new TradingCalendarDataSource() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public TradingCalendarSnapshot fetch(YearMonth month) {
                return snapshot;
            }
        };
    }

    private TradingCalendarDataSource failingSource(String name) {
        return new TradingCalendarDataSource() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public TradingCalendarSnapshot fetch(YearMonth month) {
                throw new TradingCalendarSourceException(
                        TradingCalendarFailureReason.NETWORK,
                        name + " unavailable");
            }
        };
    }

    private TradingCalendarDataSource unexpectedFailingSource(String name) {
        return new TradingCalendarDataSource() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public TradingCalendarSnapshot fetch(YearMonth month) {
                throw new IllegalStateException(name + " failed unexpectedly");
            }
        };
    }

    private TradingCalendarSnapshot snapshot() {
        return new TradingCalendarSnapshot(
                JULY_2026,
                List.of(new TradingCalendarDay(
                        JULY_2026.atDay(1), TradingDayStatus.OPEN, null)));
    }
}
