package com.stock.crawler.calendar;

import com.stock.crawler.model.TradingCalendarSnapshot;
import com.stock.crawler.model.TradingDayStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SzseTradingCalendarDataSourceTest {

    private static final YearMonth FEBRUARY_2026 = YearMonth.of(2026, 2);

    @Test
    void parsesACompleteMonth() {
        SzseTradingCalendarDataSource source = new SzseTradingCalendarDataSource(
                ignored -> monthResponse(FEBRUARY_2026, null, null));

        TradingCalendarSnapshot snapshot = source.fetch(FEBRUARY_2026);

        assertEquals(28, snapshot.days().size());
        assertEquals(TradingDayStatus.CLOSED,
                statusOn(snapshot, LocalDate.of(2026, 2, 16)));
        assertEquals(TradingDayStatus.OPEN,
                statusOn(snapshot, LocalDate.of(2026, 2, 24)));
    }

    @Test
    void rejectsAMonthWithAMissingDate() {
        SzseTradingCalendarDataSource source = new SzseTradingCalendarDataSource(
                ignored -> monthResponse(FEBRUARY_2026, LocalDate.of(2026, 2, 8), null));

        TradingCalendarSourceException exception = assertThrows(
                TradingCalendarSourceException.class,
                () -> source.fetch(FEBRUARY_2026));

        assertEquals(TradingCalendarFailureReason.INCOMPLETE_COVERAGE, exception.reason());
    }

    @Test
    void rejectsAnUnknownTradingFlag() {
        SzseTradingCalendarDataSource source = new SzseTradingCalendarDataSource(
                ignored -> monthResponse(FEBRUARY_2026, null, LocalDate.of(2026, 2, 12)));

        TradingCalendarSourceException exception = assertThrows(
                TradingCalendarSourceException.class,
                () -> source.fetch(FEBRUARY_2026));

        assertEquals(TradingCalendarFailureReason.INVALID_RESPONSE, exception.reason());
    }

    @Test
    void rejectsADuplicateDate() {
        String complete = monthResponse(FEBRUARY_2026, null, null);
        String duplicated = complete.replace(
                "],\"nowdate\"",
                "," + dayRow(LocalDate.of(2026, 2, 1), "0") + "],\"nowdate\"");
        SzseTradingCalendarDataSource source = new SzseTradingCalendarDataSource(
                ignored -> duplicated);

        TradingCalendarSourceException exception = assertThrows(
                TradingCalendarSourceException.class,
                () -> source.fetch(FEBRUARY_2026));

        assertEquals(TradingCalendarFailureReason.INCOMPLETE_COVERAGE, exception.reason());
    }

    @Test
    void rejectsAnOutOfMonthDate() {
        String response = monthResponse(FEBRUARY_2026, null, null)
                .replace("2026-02-01", "2026-01-31");
        SzseTradingCalendarDataSource source = new SzseTradingCalendarDataSource(
                ignored -> response);

        TradingCalendarSourceException exception = assertThrows(
                TradingCalendarSourceException.class,
                () -> source.fetch(FEBRUARY_2026));

        assertEquals(TradingCalendarFailureReason.INCOMPLETE_COVERAGE, exception.reason());
    }

    @Test
    void classifiesMalformedJsonAsAnInvalidResponse() {
        SzseTradingCalendarDataSource source = new SzseTradingCalendarDataSource(
                ignored -> "{not-json");

        TradingCalendarSourceException exception = assertThrows(
                TradingCalendarSourceException.class,
                () -> source.fetch(FEBRUARY_2026));

        assertEquals(TradingCalendarFailureReason.INVALID_RESPONSE, exception.reason());
    }

    private TradingDayStatus statusOn(TradingCalendarSnapshot snapshot, LocalDate date) {
        return snapshot.days().stream()
                .filter(day -> day.date().equals(date))
                .findFirst()
                .orElseThrow()
                .status();
    }

    private String monthResponse(YearMonth month, LocalDate omitted, LocalDate invalidFlagDate) {
        StringBuilder json = new StringBuilder("{\"data\":[");
        boolean first = true;
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            if (date.equals(omitted)) {
                continue;
            }
            if (!first) {
                json.append(',');
            }
            first = false;
            boolean closed = date.getDayOfWeek().getValue() >= 6
                    || (date.getDayOfMonth() >= 16 && date.getDayOfMonth() <= 23);
            String flag = date.equals(invalidFlagDate) ? "2" : closed ? "0" : "1";
            json.append(dayRow(date, flag));
        }
        return json.append("],\"nowdate\":\"2026-07-11\"}").toString();
    }

    private String dayRow(LocalDate date, String flag) {
        return "{\"zrxh\":" + date.getDayOfWeek().getValue()
                + ",\"jybz\":\"" + flag
                + "\",\"jyrq\":\"" + date + "\"}";
    }
}
