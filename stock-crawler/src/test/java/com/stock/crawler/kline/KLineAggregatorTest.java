package com.stock.crawler.kline;

import com.stock.crawler.model.KLineData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("K 线周期聚合测试")
class KLineAggregatorTest {

    @Test
    @DisplayName("月 K 应聚合 OHLC 并用前一月收盘重算涨跌幅")
    void aggregatesMonthlyBarsBeforeApplyingLimit() {
        List<KLineData> daily = List.of(
                kline("2024-01-30", "10", "12", "9", "11", 100L, "1000", "1"),
                kline("2024-01-31", "11", "13", "10", "12", null, null, null),
                kline("2024-02-01", "12", "15", "11", "14", 200L, "2000", "2"),
                kline("2024-02-29", "14", "16", "13", "15", 300L, "3000", "3"),
                kline("2024-03-01", "15", "17", "14", "16", 400L, "4000", "4")
        );

        List<KLineData> result = KLineAggregator.aggregate(daily, "month", 2);

        assertEquals(2, result.size());
        KLineData february = result.getFirst();
        assertEquals(LocalDate.of(2024, 2, 29), february.getDate());
        assertDecimal("12", february.getOpen());
        assertDecimal("16", february.getHigh());
        assertDecimal("11", february.getLow());
        assertDecimal("15", february.getClose());
        assertEquals(500L, february.getVolume());
        assertDecimal("5000", february.getAmount());
        assertDecimal("5", february.getTurnoverRate());
        assertDecimal("25.00", february.getChangePercent());

        KLineData march = result.getLast();
        assertEquals(LocalDate.of(2024, 3, 1), march.getDate());
        assertDecimal("6.67", march.getChangePercent());
        assertEquals("month", march.getPeriod());
    }

    @Test
    @DisplayName("年 K 应保留上一年度用于计算首根返回数据的涨跌幅")
    void aggregatesYearBarsWithPreviousBucketContext() {
        List<KLineData> daily = List.of(
                kline("2023-12-29", "9", "11", "8", "10", 10L, "100", "1"),
                kline("2024-01-02", "10", "13", "9", "12", 20L, "200", "2"),
                kline("2024-12-31", "12", "15", "11", "14", 30L, "300", "3")
        );

        List<KLineData> result = KLineAggregator.aggregate(daily, "year", 1);

        assertEquals(1, result.size());
        KLineData year = result.getFirst();
        assertEquals(LocalDate.of(2024, 12, 31), year.getDate());
        assertDecimal("10", year.getOpen());
        assertDecimal("15", year.getHigh());
        assertDecimal("9", year.getLow());
        assertDecimal("14", year.getClose());
        assertDecimal("40.00", year.getChangePercent());
    }

    @Test
    @DisplayName("日线需求量应包含前置周期并限制为 13000 根")
    void calculatesRequiredDailyBars() {
        assertEquals(10, KLineAggregator.requiredDailyBars("day", 10));
        assertEquals(66, KLineAggregator.requiredDailyBars("week", 10));
        assertEquals(253, KLineAggregator.requiredDailyBars("month", 10));
        assertEquals(12_750, KLineAggregator.requiredDailyBars("year", 50));
        assertEquals(13_000, KLineAggregator.requiredDailyBars("year", 100));
    }

    private KLineData kline(
            String date,
            String open,
            String high,
            String low,
            String close,
            Long volume,
            String amount,
            String turnoverRate) {
        return KLineData.builder()
                .code("sz300750")
                .date(LocalDate.parse(date))
                .open(new BigDecimal(open))
                .high(new BigDecimal(high))
                .low(new BigDecimal(low))
                .close(new BigDecimal(close))
                .volume(volume)
                .amount(amount == null ? null : new BigDecimal(amount))
                .turnoverRate(turnoverRate == null ? null : new BigDecimal(turnoverRate))
                .period("day")
                .build();
    }

    private void assertDecimal(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
