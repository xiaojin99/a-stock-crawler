package com.stock.crawler.datasource;

import com.stock.crawler.model.StockQuote;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("行情数据契约测试")
class QuoteDataContractTest {

    @Test
    @DisplayName("腾讯行情应使用供应商时间且非法可选估值保持为空")
    void tencentQuoteUsesProviderTimeAndKeepsInvalidOptionalValueNull() {
        String[] fields = tencentFields();
        fields[30] = "20260713145958";
        fields[39] = "--";
        TencentDataSource dataSource = new TencentDataSource(
                (url, policy) -> "v_sh600519=\"" + String.join("~", fields) + "\";");

        StockQuote quote = dataSource.getRealTimeQuotes(List.of("sh600519")).getFirst();

        assertEquals(LocalDateTime.of(2026, 7, 13, 14, 59, 58), quote.getTime());
        assertNull(quote.getPe());
    }

    @Test
    @DisplayName("腾讯行情必填价格非法时不应生成零价格报价")
    void tencentQuoteRejectsInvalidRequiredPrice() {
        String[] fields = tencentFields();
        fields[3] = "not-a-price";
        TencentDataSource dataSource = new TencentDataSource(
                (url, policy) -> "v_sh600519=\"" + String.join("~", fields) + "\";");

        assertTrue(dataSource.getRealTimeQuotes(List.of("sh600519")).isEmpty());
    }

    @Test
    @DisplayName("新浪行情应使用供应商日期和时间")
    void sinaQuoteUsesProviderDateAndTime() {
        String[] fields = sinaFields();
        fields[30] = "2026-07-13";
        fields[31] = "14:59:58";
        SinaDataSource dataSource = new SinaDataSource(
                (url, headers, policy) -> "var hq_str_sh600519=\"" + String.join(",", fields) + "\";");

        StockQuote quote = dataSource.getRealTimeQuotes(List.of("sh600519")).getFirst();

        assertEquals(LocalDateTime.of(2026, 7, 13, 14, 59, 58), quote.getTime());
    }

    @Test
    @DisplayName("新浪行情必填价格非法时不应生成零价格报价")
    void sinaQuoteRejectsInvalidRequiredPrice() {
        String[] fields = sinaFields();
        fields[3] = "not-a-price";
        SinaDataSource dataSource = new SinaDataSource(
                (url, headers, policy) -> "var hq_str_sh600519=\"" + String.join(",", fields) + "\";");

        assertTrue(dataSource.getRealTimeQuotes(List.of("sh600519")).isEmpty());
    }

    @Test
    @DisplayName("新浪开盘前零价格快照应跳过且不生成报价")
    void sinaQuoteSkipsPreOpenZeroPriceSnapshot() {
        String[] fields = sinaFields();
        fields[1] = "0.00";
        fields[3] = "0.00";
        fields[4] = "0.00";
        fields[5] = "0.00";
        fields[30] = "2026-07-13";
        fields[31] = "09:00:33";
        SinaDataSource dataSource = new SinaDataSource(
                (url, headers, policy) -> "var hq_str_sh600519=\"" + String.join(",", fields) + "\";");

        assertTrue(dataSource.getRealTimeQuotes(List.of("sh600519")).isEmpty());
    }

    private String[] tencentFields() {
        String[] fields = new String[47];
        Arrays.fill(fields, "");
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
        return fields;
    }

    private String[] sinaFields() {
        String[] fields = new String[32];
        Arrays.fill(fields, "0");
        fields[0] = "贵州茅台";
        fields[1] = "1260.00";
        fields[2] = "1252.10";
        fields[3] = "1271.10";
        fields[4] = "1280.00";
        fields[5] = "1250.00";
        fields[8] = "1234500";
        fields[9] = "1870400000";
        fields[10] = "1271.00";
        fields[11] = "100";
        fields[20] = "1271.10";
        fields[21] = "200";
        fields[30] = "2026-07-13";
        fields[31] = "14:59:58";
        return fields;
    }
}
