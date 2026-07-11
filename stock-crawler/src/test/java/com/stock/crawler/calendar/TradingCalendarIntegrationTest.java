package com.stock.crawler.calendar;

import com.stock.crawler.model.DataResult;
import com.stock.crawler.model.TradingCalendarSnapshot;
import com.stock.crawler.model.TradingDayStatus;
import com.stock.crawler.service.TradingCalendarService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
class TradingCalendarIntegrationTest {

    @Test
    void fetchesTheCurrentMonthFromALiveSource() {
        YearMonth month = YearMonth.now(ZoneId.of("Asia/Shanghai"));

        DataResult<TradingCalendarSnapshot> result =
                new TradingCalendarService().getTradingCalendar(month);

        assertTrue(result.isSuccess(), result.getMessage());
        assertEquals(month.lengthOfMonth(), result.getData().days().size());
        assertTrue(result.getData().days().stream()
                .noneMatch(day -> day.status() == TradingDayStatus.UNKNOWN));
    }
}
