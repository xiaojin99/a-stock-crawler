package com.stock.crawler.datasource;

import com.stock.crawler.model.KLineData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("百度 K 线解析单元测试")
class BaiduKLineDataSourceParseTest {

    @Test
    @DisplayName("应解析百度 marketData 并截取最近 N 根")
    @SuppressWarnings("unchecked")
    void parseBaiduKLineRows() throws Exception {
        BaiduKLineDataSource dataSource = new BaiduKLineDataSource();
        Method parseMethod = BaiduKLineDataSource.class.getDeclaredMethod(
                "parseKLineData", String.class, String.class, int.class, String.class);
        parseMethod.setAccessible(true);

        String body = """
                {
                  "ResultCode": "0",
                  "Result": {
                    "newMarketData": {
                      "marketData": "1717977600,2024-06-10,10.10,10.30,1000,10.50,10.00,100000,+0.20,+1.98,0.50,10.10,--,--,--,--,--,--;1718064000,2024-06-11,10.30,10.80,2000,11.00,10.20,220000,+0.50,+4.85,0.70,10.30,10.60,1500,--,--,--,--"
                    }
                  }
                }
                """;

        List<KLineData> klines = (List<KLineData>) parseMethod.invoke(
                dataSource, "sh600519", "day", 1, body);

        assertEquals(1, klines.size());
        KLineData kline = klines.getFirst();
        assertEquals("sh600519", kline.getCode());
        assertEquals(LocalDate.of(2024, 6, 11), kline.getDate());
        assertBigDecimalEquals("10.30", kline.getOpen());
        assertBigDecimalEquals("10.80", kline.getClose());
        assertBigDecimalEquals("11.00", kline.getHigh());
        assertBigDecimalEquals("10.20", kline.getLow());
        assertEquals(2000L, kline.getVolume());
        assertBigDecimalEquals("220000", kline.getAmount());
        assertBigDecimalEquals("4.85", kline.getChangePercent());
        assertBigDecimalEquals("0.70", kline.getTurnoverRate());
    }

    private void assertBigDecimalEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
