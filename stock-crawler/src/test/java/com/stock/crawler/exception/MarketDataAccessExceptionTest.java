package com.stock.crawler.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@DisplayName("行情数据访问异常测试")
class MarketDataAccessExceptionTest {

    @Test
    @DisplayName("异常应保留数据源、操作和原始原因")
    void preservesSourceOperationAndCause() {
        IOException cause = new IOException("timeout");

        MarketDataAccessException exception = new MarketDataAccessException(
                "BaiduKLine", "kline", "request failed", cause);

        assertEquals("BaiduKLine", exception.getSourceName());
        assertEquals("kline", exception.getOperation());
        assertEquals("request failed", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
