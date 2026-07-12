package com.stock.crawler.exception;

import java.io.IOException;
import java.time.Duration;

/** Indicates a provider 429 response or an active provider cooldown. */
public class ProviderRateLimitException extends IOException {

    private final String provider;
    private final Duration retryAfter;

    public ProviderRateLimitException(String provider, Duration retryAfter) {
        super("Provider cooldown is active: " + provider + ", retryAfter=" + retryAfter);
        this.provider = provider;
        this.retryAfter = retryAfter;
    }

    public String getProvider() {
        return provider;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
