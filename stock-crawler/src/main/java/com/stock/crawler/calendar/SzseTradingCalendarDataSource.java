package com.stock.crawler.calendar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.crawler.model.TradingCalendarDay;
import com.stock.crawler.model.TradingCalendarSnapshot;
import com.stock.crawler.model.TradingDayStatus;
import com.stock.crawler.util.HttpUtils;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 深交所官方逐日交易日历数据源。
 */
public class SzseTradingCalendarDataSource implements TradingCalendarDataSource {

    private static final String URL_TEMPLATE =
            "https://www.szse.cn/api/report/exchange/onepersistenthour/monthList?month=%s";
    private static final Map<String, String> HEADERS = Map.of(
            "Accept", "application/json, text/plain, */*",
            "Referer", "https://www.szse.cn/aboutus/calendar/"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CalendarBodyFetcher bodyFetcher;

    public SzseTradingCalendarDataSource() {
        this(url -> HttpUtils.get(url, HEADERS));
    }

    SzseTradingCalendarDataSource(CalendarBodyFetcher bodyFetcher) {
        this.bodyFetcher = bodyFetcher;
    }

    @Override
    public String name() {
        return "szse";
    }

    @Override
    public TradingCalendarSnapshot fetch(YearMonth month) {
        if (month == null) {
            throw new IllegalArgumentException("month must not be null");
        }
        String url = URL_TEMPLATE.formatted(month);
        String body;
        try {
            body = bodyFetcher.fetch(url);
        } catch (IOException ex) {
            throw new TradingCalendarSourceException(
                    TradingCalendarFailureReason.NETWORK,
                    "SZSE trading calendar request failed for " + month,
                    ex);
        }

        try {
            return parse(month, body);
        } catch (TradingCalendarSourceException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new TradingCalendarSourceException(
                    TradingCalendarFailureReason.INVALID_RESPONSE,
                    "SZSE trading calendar response is invalid for " + month,
                    ex);
        } catch (RuntimeException ex) {
            throw new TradingCalendarSourceException(
                    TradingCalendarFailureReason.INVALID_RESPONSE,
                    "SZSE trading calendar response is invalid for " + month,
                    ex);
        }
    }

    private TradingCalendarSnapshot parse(YearMonth month, String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        JsonNode data = root.path("data");
        if (!data.isArray()) {
            throw invalid("SZSE response does not contain a data array for " + month);
        }

        Map<LocalDate, TradingCalendarDay> daysByDate = new HashMap<>();
        for (JsonNode row : data) {
            LocalDate date = parseDate(row.path("jyrq").asText(null), month);
            TradingDayStatus status = parseStatus(row.path("jybz").asText(null), date);
            String reason = status == TradingDayStatus.CLOSED
                    ? closedReason(date)
                    : null;
            TradingCalendarDay previous = daysByDate.put(
                    date, new TradingCalendarDay(date, status, reason));
            if (previous != null) {
                throw incomplete("SZSE response contains duplicate date " + date);
            }
        }

        List<TradingCalendarDay> days = new ArrayList<>(month.lengthOfMonth());
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            TradingCalendarDay calendarDay = daysByDate.get(date);
            if (calendarDay == null) {
                throw incomplete("SZSE response is missing date " + date);
            }
            days.add(calendarDay);
        }
        if (daysByDate.size() != month.lengthOfMonth()) {
            throw incomplete("SZSE response contains dates outside " + month);
        }
        return new TradingCalendarSnapshot(month, days);
    }

    private LocalDate parseDate(String value, YearMonth expectedMonth) {
        if (value == null || value.isBlank()) {
            throw invalid("SZSE response contains an empty trading date");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(value);
        } catch (RuntimeException ex) {
            throw new TradingCalendarSourceException(
                    TradingCalendarFailureReason.INVALID_RESPONSE,
                    "SZSE response contains invalid trading date " + value,
                    ex);
        }
        if (!YearMonth.from(date).equals(expectedMonth)) {
            throw incomplete("SZSE response contains out-of-month date " + date);
        }
        return date;
    }

    private TradingDayStatus parseStatus(String value, LocalDate date) {
        return switch (value) {
            case "1" -> TradingDayStatus.OPEN;
            case "0" -> TradingDayStatus.CLOSED;
            default -> throw invalid(
                    "SZSE response contains unknown trading flag " + value + " for " + date);
        };
    }

    private String closedReason(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY
                ? "WEEKEND"
                : "MARKET_CLOSED";
    }

    private TradingCalendarSourceException invalid(String message) {
        return new TradingCalendarSourceException(
                TradingCalendarFailureReason.INVALID_RESPONSE, message);
    }

    private TradingCalendarSourceException incomplete(String message) {
        return new TradingCalendarSourceException(
                TradingCalendarFailureReason.INCOMPLETE_COVERAGE, message);
    }

    @FunctionalInterface
    interface CalendarBodyFetcher {
        String fetch(String url) throws IOException;
    }
}
