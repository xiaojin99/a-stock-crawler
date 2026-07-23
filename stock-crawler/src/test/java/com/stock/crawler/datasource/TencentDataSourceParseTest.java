package com.stock.crawler.datasource;

import com.stock.crawler.model.StockQuote;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("腾讯行情解析单元测试")
class TencentDataSourceParseTest {

    @Test
    @DisplayName("字段索引应按腾讯财经实测格式映射")
    @SuppressWarnings("unchecked")
    void parseTencentQuoteFields() throws Exception {
        TencentDataSource dataSource = new TencentDataSource();
        Method parseMethod = TencentDataSource.class.getDeclaredMethod("parseTencentQuotes", String.class);
        parseMethod.setAccessible(true);

        String[] fields = new String[53];
        for (int i = 0; i < fields.length; i++) {
            fields[i] = "";
        }
        fields[0] = "1";
        fields[1] = "贵州茅台";
        fields[2] = "600519";
        fields[3] = "1271.10";
        fields[4] = "1252.10";
        fields[5] = "1260.00";
        fields[6] = "12345";
        fields[30] = "20260713145958";
        fields[31] = "19.00";
        fields[32] = "1.52";
        fields[33] = "1280.00";
        fields[34] = "1250.00";
        fields[37] = "187040";
        fields[38] = "0.55";
        fields[39] = "23.45";
        fields[44] = "410.88";
        fields[46] = "11.51";

        String body = "v_sh600519=\"" + String.join("~", fields) + "\";";
        List<StockQuote> quotes = (List<StockQuote>) parseMethod.invoke(dataSource, body);

        assertEquals(1, quotes.size());
        StockQuote quote = quotes.getFirst();
        assertEquals("sh600519", quote.getCode());
        assertEquals("贵州茅台", quote.getName());
        assertBigDecimalEquals("1271.10", quote.getPrice());
        assertBigDecimalEquals("1252.10", quote.getPreClose());
        assertBigDecimalEquals("1260.00", quote.getOpen());
        assertBigDecimalEquals("1280.00", quote.getHigh());
        assertBigDecimalEquals("1250.00", quote.getLow());
        assertEquals(1_234_500L, quote.getVolume());
        assertBigDecimalEquals("1870400000", quote.getAmount());
        assertBigDecimalEquals("19.00", quote.getChange());
        assertBigDecimalEquals("1.52", quote.getChangePercent());
        assertBigDecimalEquals("0.55", quote.getTurnoverRate());
        assertBigDecimalEquals("23.45", quote.getPe());
        assertBigDecimalEquals("41088000000", quote.getMarketCap());
        assertBigDecimalEquals("11.51", quote.getPb());
    }

    @Test
    @DisplayName("尾部估值字段为空时仍应保留有效报价")
    @SuppressWarnings("unchecked")
    void parseTencentQuoteWithBlankTrailingValuationFields() throws Exception {
        TencentDataSource dataSource = new TencentDataSource();
        Method parseMethod = TencentDataSource.class.getDeclaredMethod("parseTencentQuotes", String.class);
        parseMethod.setAccessible(true);

        String[] fields = new String[47];
        for (int i = 0; i < fields.length; i++) {
            fields[i] = "";
        }
        fields[0] = "1";
        fields[1] = "贵州茅台";
        fields[2] = "600519";
        fields[3] = "1271.10";
        fields[4] = "1291.91";
        fields[5] = "1292.70";
        fields[6] = "41586";
        fields[30] = "20260713145958";
        fields[31] = "-20.81";
        fields[32] = "-1.61";
        fields[33] = "1292.70";
        fields[34] = "1270.10";
        fields[37] = "530366";

        String body = "v_sh600519=\"" + String.join("~", fields) + "\";";
        List<StockQuote> quotes = (List<StockQuote>) parseMethod.invoke(dataSource, body);

        assertEquals(1, quotes.size());
        StockQuote quote = quotes.getFirst();
        assertEquals("sh600519", quote.getCode());
        assertBigDecimalEquals("1271.10", quote.getPrice());
        assertNull(quote.getPe());
        assertNull(quote.getPb());
        assertNull(quote.getMarketCap());
    }

    private void assertBigDecimalEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
