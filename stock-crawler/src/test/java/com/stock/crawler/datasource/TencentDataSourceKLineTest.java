package com.stock.crawler.datasource;

import com.stock.crawler.model.KLineData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("腾讯指数 K 线解析单元测试")
class TencentDataSourceKLineTest {

    @Test
    @DisplayName("应解析腾讯 day 数组并按 date/open/close/high/low 正确映射")
    @SuppressWarnings("unchecked")
    void parseTencentIndexKLineRows() throws Exception {
        TencentDataSource dataSource = new TencentDataSource();
        Method parseMethod = TencentDataSource.class.getDeclaredMethod(
                "parseTencentKLineData", String.class, String.class, String.class);
        parseMethod.setAccessible(true);

        // 腾讯返回字段顺序：[date, open, close, high, low, volume]
        String body = """
                {
                  "code": 0,
                  "msg": "",
                  "data": {
                    "sh000985": {
                      "day": [
                        ["2026-07-10", "4900.100", "4800.500", "4950.000", "4780.200", "1000000.000"],
                        ["2026-07-11", "4800.500", "4850.300", "4880.000", "4790.000", "1100000.000"]
                      ]
                    }
                  }
                }
                """;

        List<KLineData> klines = (List<KLineData>) parseMethod.invoke(
                dataSource, "sh000985", "day", body);

        assertEquals(2, klines.size());
        KLineData kline = klines.get(1);
        assertEquals("sh000985", kline.getCode());
        assertEquals(LocalDate.of(2026, 7, 11), kline.getDate());
        assertBigDecimalEquals("4800.500", kline.getOpen());
        assertBigDecimalEquals("4850.300", kline.getClose());
        assertBigDecimalEquals("4880.000", kline.getHigh());
        assertBigDecimalEquals("4790.000", kline.getLow());
        assertEquals(1100000L, kline.getVolume());
        assertEquals("day", kline.getPeriod());
    }

    @Test
    @DisplayName("缺少 day 数组时返回空列表")
    @SuppressWarnings("unchecked")
    void parseTencentKLineEmptyWhenMissingDayArray() throws Exception {
        TencentDataSource dataSource = new TencentDataSource();
        Method parseMethod = TencentDataSource.class.getDeclaredMethod(
                "parseTencentKLineData", String.class, String.class, String.class);
        parseMethod.setAccessible(true);

        List<KLineData> klines = (List<KLineData>) parseMethod.invoke(
                dataSource, "sh000985", "day", "{\"code\":0,\"data\":{}}");

        assertTrue(klines.isEmpty());
    }

    @Test
    @DisplayName("仅声明支持日 K 周期")
    void supportsDayKLineOnly() {
        TencentDataSource dataSource = new TencentDataSource();
        assertTrue(dataSource.supportsKLinePeriod("day"));
        assertTrue(dataSource.supportsKLinePeriod("DAY"));
        assertFalse(dataSource.supportsKLinePeriod("month"));
        assertFalse(dataSource.supportsKLinePeriod("year"));
    }

    private static void assertBigDecimalEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
