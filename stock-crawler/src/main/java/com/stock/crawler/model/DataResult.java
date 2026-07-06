package com.stock.crawler.model;

import java.io.Serializable;
import java.time.Instant;

/**
 * 数据能力统一返回契约。
 *
 * <p>调用方可以通过 success/message/source/elapsedMs 判断是否需要降级，
 * 避免把数据源异常直接扩散到业务层。</p>
 */
public class DataResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;
    private T data;
    private String source;
    private String message;
    private long elapsedMs;
    private Instant fetchedAt;
    private boolean stale;

    public DataResult() {
    }

    public static <T> DataResult<T> success(T data, String source, long elapsedMs) {
        DataResult<T> result = new DataResult<>();
        result.success = true;
        result.data = data;
        result.source = source;
        result.message = "OK";
        result.elapsedMs = elapsedMs;
        result.fetchedAt = Instant.now();
        result.stale = false;
        return result;
    }

    public static <T> DataResult<T> failure(String source, String message, long elapsedMs) {
        DataResult<T> result = new DataResult<>();
        result.success = false;
        result.source = source;
        result.message = message;
        result.elapsedMs = elapsedMs;
        result.fetchedAt = Instant.now();
        result.stale = false;
        return result;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public long getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(long elapsedMs) { this.elapsedMs = elapsedMs; }
    public Instant getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(Instant fetchedAt) { this.fetchedAt = fetchedAt; }
    public boolean isStale() { return stale; }
    public void setStale(boolean stale) { this.stale = stale; }
}
