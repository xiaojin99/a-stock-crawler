package com.stock.crawler.calendar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.crawler.model.TradingCalendarDay;
import com.stock.crawler.model.TradingCalendarSnapshot;
import com.stock.crawler.model.TradingDayStatus;
import com.stock.crawler.util.CrawlerRequestPolicy;
import com.stock.crawler.util.HttpUtils;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 东方财富休市区间备用数据源。
 */
public class EastMoneyTradingCalendarDataSource implements TradingCalendarDataSource {

    private static final String URL_TEMPLATE =
            "https://datacenter-web.eastmoney.com/api/data/v1/get"
                    + "?reportName=RPTA_WEB_ZGXSRL&columns=ALL&pageSize=200"
                    + "&pageNumber=%d&sortColumns=SDATE&sortTypes=-1";
    private static final Map<String, String> HEADERS = Map.of(
            "Referer", "https://data.eastmoney.com/dcrl/close.html"
    );
    private static final int MAX_PAGES = 10;
    private static final List<String> REQUIRED_HOLIDAY_TOKENS = List.of(
            "元旦", "春节", "清明", "劳动", "端午", "中秋", "国庆");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CalendarBodyFetcher bodyFetcher;

    public EastMoneyTradingCalendarDataSource() {
        this(url -> HttpUtils.getEastMoney(
                url, HEADERS, CrawlerRequestPolicy.backgroundNews()));
    }

    EastMoneyTradingCalendarDataSource(CalendarBodyFetcher bodyFetcher) {
        this.bodyFetcher = bodyFetcher;
    }

    @Override
    public String name() {
        return "eastmoney";
    }

    @Override
    public TradingCalendarSnapshot fetch(YearMonth month) {
        if (month == null) {
            throw new IllegalArgumentException("month must not be null");
        }
        try {
            List<ClosureRange> ranges = fetchAllPages();
            validateAnnualCoverage(month.getYear(), ranges);
            validateSessions(month, ranges);
            return buildSnapshot(month, ranges);
        } catch (TradingCalendarSourceException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new TradingCalendarSourceException(
                    TradingCalendarFailureReason.INVALID_RESPONSE,
                    "EastMoney trading calendar response is invalid for " + month,
                    ex);
        }
    }

    private List<ClosureRange> fetchAllPages() {
        Page firstPage = fetchPage(1);
        List<ClosureRange> ranges = new ArrayList<>(firstPage.ranges());
        for (int pageNumber = 2; pageNumber <= firstPage.pages(); pageNumber++) {
            Page page = fetchPage(pageNumber);
            ranges.addAll(page.ranges());
        }
        return ranges;
    }

    private Page fetchPage(int pageNumber) {
        String body;
        try {
            body = bodyFetcher.fetch(URL_TEMPLATE.formatted(pageNumber));
        } catch (IOException ex) {
            throw new TradingCalendarSourceException(
                    TradingCalendarFailureReason.NETWORK,
                    "EastMoney trading calendar request failed on page " + pageNumber,
                    ex);
        }

        try {
            return parsePage(body);
        } catch (TradingCalendarSourceException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new TradingCalendarSourceException(
                    TradingCalendarFailureReason.INVALID_RESPONSE,
                    "EastMoney trading calendar response is invalid on page " + pageNumber,
                    ex);
        }
    }

    private Page parsePage(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        if (!root.path("success").asBoolean(false) || root.path("code").asInt(-1) != 0) {
            throw invalid("EastMoney response reports failure");
        }
        JsonNode result = root.path("result");
        JsonNode data = result.path("data");
        if (!data.isArray()) {
            throw invalid("EastMoney response does not contain a data array");
        }
        int pages = result.path("pages").asInt(0);
        if (pages < 1 || pages > MAX_PAGES) {
            throw invalid("EastMoney response contains invalid page count " + pages);
        }

        List<ClosureRange> ranges = new ArrayList<>();
        for (JsonNode row : data) {
            if (!"A股".equals(row.path("MKT").asText())) {
                continue;
            }
            String holiday = row.path("HOLIDAY").asText(null);
            LocalDate startDate = parseDate(row.path("SDATE").asText(null));
            LocalDate endDate = parseDate(row.path("EDATE").asText(null));
            if (holiday == null || holiday.isBlank() || endDate.isBefore(startDate)) {
                throw invalid("EastMoney response contains an invalid closure range");
            }
            String halfDayFlag = row.path("XS").isNull()
                    ? null
                    : row.path("XS").asText(null);
            ranges.add(new ClosureRange(
                    holiday, startDate, endDate, "1".equals(halfDayFlag)));
        }
        return new Page(pages, ranges);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.length() < 10) {
            throw invalid("EastMoney response contains an invalid date " + value);
        }
        try {
            return LocalDate.parse(value.substring(0, 10));
        } catch (RuntimeException ex) {
            throw new TradingCalendarSourceException(
                    TradingCalendarFailureReason.INVALID_RESPONSE,
                    "EastMoney response contains an invalid date " + value,
                    ex);
        }
    }

    private void validateAnnualCoverage(int year, List<ClosureRange> ranges) {
        Set<String> matchedTokens = new HashSet<>();
        for (ClosureRange range : ranges) {
            if (!overlapsYear(range, year)) {
                continue;
            }
            for (String token : REQUIRED_HOLIDAY_TOKENS) {
                if (range.holiday().contains(token)) {
                    matchedTokens.add(token);
                }
            }
        }
        if (!matchedTokens.containsAll(REQUIRED_HOLIDAY_TOKENS)) {
            Set<String> missing = new HashSet<>(REQUIRED_HOLIDAY_TOKENS);
            missing.removeAll(matchedTokens);
            throw new TradingCalendarSourceException(
                    TradingCalendarFailureReason.INCOMPLETE_COVERAGE,
                    "EastMoney annual calendar is incomplete for " + year + ": missing " + missing);
        }
    }

    private boolean overlapsYear(ClosureRange range, int year) {
        return range.startDate().getYear() <= year && range.endDate().getYear() >= year;
    }

    private void validateSessions(YearMonth month, List<ClosureRange> ranges) {
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();
        boolean unsupported = ranges.stream()
                .anyMatch(range -> range.halfDay()
                        && !range.endDate().isBefore(monthStart)
                        && !range.startDate().isAfter(monthEnd));
        if (unsupported) {
            throw new TradingCalendarSourceException(
                    TradingCalendarFailureReason.UNSUPPORTED_SESSION,
                    "EastMoney returned a half-day closure without session details for " + month);
        }
    }

    private TradingCalendarSnapshot buildSnapshot(
            YearMonth month,
            List<ClosureRange> ranges) {
        List<TradingCalendarDay> days = new ArrayList<>(month.lengthOfMonth());
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            ClosureRange closure = ranges.stream()
                    .filter(range -> !date.isBefore(range.startDate())
                            && !date.isAfter(range.endDate()))
                    .findFirst()
                    .orElse(null);
            boolean weekend = date.getDayOfWeek() == DayOfWeek.SATURDAY
                    || date.getDayOfWeek() == DayOfWeek.SUNDAY;
            TradingDayStatus status = weekend || closure != null
                    ? TradingDayStatus.CLOSED
                    : TradingDayStatus.OPEN;
            String reason = closure != null
                    ? closure.holiday()
                    : weekend ? "WEEKEND" : null;
            days.add(new TradingCalendarDay(date, status, reason));
        }
        return new TradingCalendarSnapshot(month, days);
    }

    private TradingCalendarSourceException invalid(String message) {
        return new TradingCalendarSourceException(
                TradingCalendarFailureReason.INVALID_RESPONSE, message);
    }

    private record ClosureRange(
            String holiday,
            LocalDate startDate,
            LocalDate endDate,
            boolean halfDay) {
    }

    private record Page(int pages, List<ClosureRange> ranges) {
    }

    @FunctionalInterface
    interface CalendarBodyFetcher {
        String fetch(String url) throws IOException;
    }
}
