package com.stock.crawler.exception;

import java.io.IOException;
import java.time.Duration;

/** Indicates that a provider call was rejected by the local circuit breaker. */
public class ProviderCircuitOpenException extends IOException {

    private final String provider;
    private final Duration retryAfter;

    public ProviderCircuitOpenException(String provider, Duration retryAfter) {
        super("Provider circuit is open: " + provider + ", retryAfter=" + retryAfter);
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
