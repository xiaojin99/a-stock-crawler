package com.stock.crawler.service;

import com.stock.crawler.exception.ProviderRateLimitException;
import java.time.Duration;

/** Unchecked market-news signal used to coordinate cross-instance provider cooldowns. */
public class MarketNewsRateLimitException extends RuntimeException {

    private final String provider;
    private final Duration retryAfter;

    public MarketNewsRateLimitException(ProviderRateLimitException cause) {
        super(cause.getMessage(), cause);
        this.provider = cause.getProvider();
        this.retryAfter = cause.getRetryAfter();
    }

    public String getProvider() {
        return provider;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
