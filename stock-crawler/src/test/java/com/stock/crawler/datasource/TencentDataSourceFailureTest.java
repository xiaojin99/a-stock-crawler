package com.stock.crawler.datasource;

import com.stock.crawler.exception.MarketDataAccessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("腾讯行情失败语义测试")
class TencentDataSourceFailureTest {

    @Test
    @DisplayName("网络超时应包装为行情访问异常并保留原因")
    void wrapsNetworkTimeoutAsMarketDataAccessException() {
        SocketTimeoutException timeout = new SocketTimeoutException("simulated timeout");
        TencentDataSource dataSource = new TencentDataSource(url -> {
            throw timeout;
        });

        MarketDataAccessException exception = assertThrows(
                MarketDataAccessException.class,
                () -> dataSource.getRealTimeQuotes(List.of("sz300750")));

        assertEquals("Tencent", exception.getSourceName());
        assertEquals("quote", exception.getOperation());
        assertSame(timeout, exception.getCause());
    }

    @Test
    @DisplayName("非空非法响应应报告解析失败")
    void rejectsMalformedNonEmptyResponse() {
        TencentDataSource dataSource = new TencentDataSource(url -> "<html>rate limited</html>");

        MarketDataAccessException exception = assertThrows(
                MarketDataAccessException.class,
                () -> dataSource.getRealTimeQuotes(List.of("sz300750")));

        assertEquals("Tencent", exception.getSourceName());
        assertEquals("quote", exception.getOperation());
    }

    @Test
    @DisplayName("合法无匹配响应应保持空结果而不是健康失败")
    void keepsNoMatchResponseAsEmptyResult() {
        TencentDataSource dataSource = new TencentDataSource(url -> "v_pv_none_match=\"1\";");

        assertTrue(dataSource.getRealTimeQuotes(List.of("sz999999")).isEmpty());
    }

    @Test
    @DisplayName("空代码列表不应发起网络请求")
    void skipsRequestForEmptyCodes() {
        AtomicInteger calls = new AtomicInteger();
        TencentDataSource dataSource = new TencentDataSource(url -> {
            calls.incrementAndGet();
            return "";
        });

        assertTrue(dataSource.getRealTimeQuotes(List.of()).isEmpty());
        assertEquals(0, calls.get());
    }
}
