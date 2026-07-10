package com.stock.crawler.exception;

/**
 * 行情数据源发生网络、超时或响应解析错误。
 */
public class MarketDataAccessException extends RuntimeException {

    private final String sourceName;
    private final String operation;

    public MarketDataAccessException(
            String sourceName,
            String operation,
            String message,
            Throwable cause) {
        super(message, cause);
        this.sourceName = sourceName;
        this.operation = operation;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getOperation() {
        return operation;
    }
}
