package com.stock.crawler.calendar;

import com.stock.crawler.model.TradingCalendarSnapshot;
import com.stock.crawler.model.TradingDayStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EastMoneyTradingCalendarDataSourceTest {

    private static final YearMonth FEBRUARY_2026 = YearMonth.of(2026, 2);

    @Test
    void expandsClosedRangesAfterValidatingTheAnnualSchedule() {
        EastMoneyTradingCalendarDataSource source = new EastMoneyTradingCalendarDataSource(
                ignored -> completeYearResponse(null));

        TradingCalendarSnapshot snapshot = source.fetch(FEBRUARY_2026);

        assertEquals(28, snapshot.days().size());
        assertEquals(TradingDayStatus.CLOSED,
                statusOn(snapshot, LocalDate.of(2026, 2, 16)));
        assertEquals("春节", reasonOn(snapshot, LocalDate.of(2026, 2, 16)));
        assertEquals(TradingDayStatus.OPEN,
                statusOn(snapshot, LocalDate.of(2026, 2, 24)));
    }

    @Test
    void rejectsAnIncompleteAnnualSchedule() {
        EastMoneyTradingCalendarDataSource source = new EastMoneyTradingCalendarDataSource(
                ignored -> incompleteYearResponse());

        TradingCalendarSourceException exception = assertThrows(
                TradingCalendarSourceException.class,
                () -> source.fetch(FEBRUARY_2026));

        assertEquals(TradingCalendarFailureReason.INCOMPLETE_COVERAGE, exception.reason());
    }

    @Test
    void rejectsHalfDayClosuresThatOverlapTheRequestedMonth() {
        EastMoneyTradingCalendarDataSource source = new EastMoneyTradingCalendarDataSource(
                ignored -> completeYearResponse("1"));

        TradingCalendarSourceException exception = assertThrows(
                TradingCalendarSourceException.class,
                () -> source.fetch(FEBRUARY_2026));

        assertEquals(TradingCalendarFailureReason.UNSUPPORTED_SESSION, exception.reason());
    }

    @Test
    void fetchesAllPagesBeforeValidatingCoverage() {
        AtomicInteger calls = new AtomicInteger();
        EastMoneyTradingCalendarDataSource source = new EastMoneyTradingCalendarDataSource(url -> {
            calls.incrementAndGet();
            if (url.contains("pageNumber=1")) {
                return response(2,
                        row("元旦", "2026-01-01", "2026-01-02", null),
                        row("春节", "2026-02-16", "2026-02-23", null),
                        row("清明节", "2026-04-06", "2026-04-06", null),
                        row("劳动节", "2026-05-01", "2026-05-05", null));
            }
            return response(2,
                    row("端午节", "2026-06-19", "2026-06-19", null),
                    row("中秋节", "2026-09-25", "2026-09-25", null),
                    row("国庆节", "2026-10-01", "2026-10-07", null));
        });

        TradingCalendarSnapshot snapshot = source.fetch(FEBRUARY_2026);

        assertEquals(2, calls.get());
        assertEquals(28, snapshot.days().size());
    }

    @Test
    void acceptsCombinedMidAutumnAndNationalDayRecords() {
        EastMoneyTradingCalendarDataSource source = new EastMoneyTradingCalendarDataSource(
                ignored -> response(
                        row("元旦", "2026-01-01", "2026-01-02", null),
                        row("春节", "2026-02-16", "2026-02-23", null),
                        row("清明节", "2026-04-06", "2026-04-06", null),
                        row("劳动节", "2026-05-01", "2026-05-05", null),
                        row("端午节", "2026-06-19", "2026-06-19", null),
                        row("国庆节、中秋节", "2026-10-01", "2026-10-07", null)));

        TradingCalendarSnapshot snapshot = source.fetch(FEBRUARY_2026);

        assertEquals(TradingDayStatus.OPEN,
                statusOn(snapshot, LocalDate.of(2026, 2, 24)));
    }

    @Test
    void classifiesMalformedJsonAsAnInvalidResponse() {
        EastMoneyTradingCalendarDataSource source = new EastMoneyTradingCalendarDataSource(
                ignored -> "{not-json");

        TradingCalendarSourceException exception = assertThrows(
                TradingCalendarSourceException.class,
                () -> source.fetch(FEBRUARY_2026));

        assertEquals(TradingCalendarFailureReason.INVALID_RESPONSE, exception.reason());
    }

    @Test
    void rejectsAnExcessivePageCountBeforeFetchingMorePages() {
        AtomicInteger calls = new AtomicInteger();
        EastMoneyTradingCalendarDataSource source = new EastMoneyTradingCalendarDataSource(
                ignored -> {
                    calls.incrementAndGet();
                    return response(11,
                            row("元旦", "2026-01-01", "2026-01-02", null));
                });

        TradingCalendarSourceException exception = assertThrows(
                TradingCalendarSourceException.class,
                () -> source.fetch(FEBRUARY_2026));

        assertEquals(TradingCalendarFailureReason.INVALID_RESPONSE, exception.reason());
        assertEquals(1, calls.get());
    }

    private TradingDayStatus statusOn(TradingCalendarSnapshot snapshot, LocalDate date) {
        return snapshot.days().stream()
                .filter(day -> day.date().equals(date))
                .findFirst()
                .orElseThrow()
                .status();
    }

    private String reasonOn(TradingCalendarSnapshot snapshot, LocalDate date) {
        return snapshot.days().stream()
                .filter(day -> day.date().equals(date))
                .findFirst()
                .orElseThrow()
                .reason();
    }

    private String completeYearResponse(String springFestivalHalfDayFlag) {
        return response(
                row("元旦", "2026-01-01", "2026-01-02", null),
                row("春节", "2026-02-16", "2026-02-23", springFestivalHalfDayFlag),
                row("清明节", "2026-04-06", "2026-04-06", null),
                row("劳动节", "2026-05-01", "2026-05-05", null),
                row("端午节", "2026-06-19", "2026-06-19", null),
                row("中秋节", "2026-09-25", "2026-09-25", null),
                row("国庆节", "2026-10-01", "2026-10-07", null));
    }

    private String incompleteYearResponse() {
        return response(row("春节", "2026-02-16", "2026-02-23", null));
    }

    private String response(String... rows) {
        return response(1, rows);
    }

    private String response(int pages, String... rows) {
        return "{\"version\":\"v1\",\"result\":{\"pages\":1,\"data\":["
                .replace("\"pages\":1", "\"pages\":" + pages)
                + String.join(",", rows)
                + "],\"count\":" + rows.length
                + "},\"success\":true,\"message\":\"ok\",\"code\":0}";
    }

    private String row(String holiday, String start, String end, String halfDayFlag) {
        String xs = halfDayFlag == null ? "null" : "\"" + halfDayFlag + "\"";
        return "{\"MKT\":\"A股\",\"HOLIDAY\":\"" + holiday
                + "\",\"SDATE\":\"" + start + " 00:00:00\",\"EDATE\":\""
                + end + " 00:00:00\",\"XS\":" + xs + ",\"FIRST_LETTER\":\"A\"}";
    }
}
