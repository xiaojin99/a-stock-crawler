package com.stock.crawler.util;

import com.stock.crawler.exception.ProviderCircuitOpenException;
import com.stock.crawler.exception.ProviderRateLimitException;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** HTTP utility with per-operation deadlines, restricted retries, and provider cooldowns. */
public final class HttpUtils {

    private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .followRedirects(true)
            .retryOnConnectionFailure(false)
            .build();

    private static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";

    /** GBK charset used by Tencent Finance responses. */
    public static final Charset GBK = Charset.forName("GBK");

    private static final long RETRY_DELAY_MS = 250L;
    private static final Duration DEFAULT_RATE_LIMIT_COOLDOWN = Duration.ofMinutes(5);
    private static final Duration CIRCUIT_OPEN_DURATION = Duration.ofSeconds(60);
    private static final int CIRCUIT_FAILURE_THRESHOLD = 3;
    private static final Object EASTMONEY_LOCK = new Object();
    private static final long EASTMONEY_MIN_INTERVAL_MS =
            Long.getLong("stockcrawler.eastmoney.minIntervalMs", 2000L);
    private static final Map<String, Long> COOLDOWN_UNTIL_MS = new ConcurrentHashMap<>();
    private static final Map<String, CircuitState> CIRCUIT_STATES = new ConcurrentHashMap<>();
    private static volatile LongSupplier currentTimeMs = System::currentTimeMillis;
    private static volatile long eastMoneyLastCallAtMs;

    private HttpUtils() {}

    /** Executes a GET request with the compatibility policy. */
    public static String get(String url) throws IOException {
        return get(url, null, CrawlerRequestPolicy.legacyDefault());
    }

    /** Executes a GET request with headers and the compatibility policy. */
    public static String get(String url, Map<String, String> headers) throws IOException {
        return get(url, headers, CrawlerRequestPolicy.legacyDefault());
    }

    /** Executes a GET request with explicit timeout and attempt bounds. */
    public static String get(
            String url,
            Map<String, String> headers,
            CrawlerRequestPolicy policy) throws IOException {
        Request.Builder builder = defaultGetBuilder(url);
        addHeaders(builder, headers);
        return new String(executeBytes(builder.build(), url, policy), StandardCharsets.UTF_8);
    }

    /** Executes an EastMoney GET through the shared EastMoney throttle. */
    public static String getEastMoney(String url, Map<String, String> headers) throws IOException {
        return getEastMoney(url, headers, CrawlerRequestPolicy.legacyDefault());
    }

    /** Executes an EastMoney GET with explicit timeout and attempt bounds. */
    public static String getEastMoney(
            String url,
            Map<String, String> headers,
            CrawlerRequestPolicy policy) throws IOException {
        Request.Builder builder = defaultGetBuilder(url)
                .header("Accept", "application/json, text/plain, */*")
                .header("Referer", "https://quote.eastmoney.com/");
        addHeaders(builder, headers);
        return new String(executeBytes(builder.build(), url, policy), StandardCharsets.UTF_8);
    }

    /** Executes a GET and decodes the response with the requested charset. */
    public static String getWithCharset(String url, Charset charset) throws IOException {
        return getWithCharset(url, charset, CrawlerRequestPolicy.legacyDefault());
    }

    /** Executes a charset-aware GET with explicit timeout and attempt bounds. */
    public static String getWithCharset(
            String url,
            Charset charset,
            CrawlerRequestPolicy policy) throws IOException {
        return getWithCharset(url, null, charset, policy);
    }

    /** Executes a charset-aware GET with headers and explicit timeout and attempt bounds. */
    public static String getWithCharset(
            String url,
            Map<String, String> headers,
            Charset charset,
            CrawlerRequestPolicy policy) throws IOException {
        Request.Builder builder = defaultGetBuilder(url);
        addHeaders(builder, headers);
        return new String(executeBytes(builder.build(), url, policy), charset);
    }

    /** Downloads bytes with the compatibility policy. */
    public static byte[] getBytes(String url) throws IOException {
        return getBytes(url, CrawlerRequestPolicy.legacyDefault());
    }

    /** Downloads bytes with explicit timeout and attempt bounds. */
    public static byte[] getBytes(String url, CrawlerRequestPolicy policy) throws IOException {
        return executeBytes(defaultGetBuilder(url).build(), url, policy);
    }

    /** Returns the shared base client for legacy integrations. */
    public static OkHttpClient getClient() {
        return CLIENT;
    }

    /** Executes a JSON POST with the compatibility policy. */
    public static String post(String url, String jsonBody) throws IOException {
        return post(url, jsonBody, null, CrawlerRequestPolicy.legacyDefault());
    }

    /** Executes a JSON POST with headers and the compatibility policy. */
    public static String post(
            String url,
            String jsonBody,
            Map<String, String> headers) throws IOException {
        return post(url, jsonBody, headers, CrawlerRequestPolicy.legacyDefault());
    }

    /** Executes a JSON POST with explicit timeout and attempt bounds. */
    public static String post(
            String url,
            String jsonBody,
            Map<String, String> headers,
            CrawlerRequestPolicy policy) throws IOException {
        MediaType json = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonBody, json);
        Request.Builder builder = defaultRequestBuilder(url)
                .post(body)
                .header("Accept", "application/json, text/plain, */*");
        addHeaders(builder, headers);
        return new String(executeBytes(builder.build(), url, policy), StandardCharsets.UTF_8);
    }

    /** Executes a form POST with the compatibility policy. */
    public static String postForm(
            String url,
            Map<String, String> form,
            Map<String, String> headers) throws IOException {
        return postForm(url, form, headers, CrawlerRequestPolicy.legacyDefault());
    }

    /** Executes a form POST with explicit timeout and attempt bounds. */
    public static String postForm(
            String url,
            Map<String, String> form,
            Map<String, String> headers,
            CrawlerRequestPolicy policy) throws IOException {
        FormBody.Builder formBuilder = new FormBody.Builder();
        if (form != null) {
            form.forEach((key, value) -> formBuilder.add(key, value == null ? "" : value));
        }
        Request.Builder builder = defaultRequestBuilder(url)
                .post(formBuilder.build())
                .header("Accept", "application/json, text/plain, */*");
        addHeaders(builder, headers);
        return new String(executeBytes(builder.build(), url, policy), StandardCharsets.UTF_8);
    }

    static void clearCooldownsForTest() {
        COOLDOWN_UNTIL_MS.clear();
    }

    static void resetProviderStateForTest(LongSupplier timeSupplier) {
        COOLDOWN_UNTIL_MS.clear();
        CIRCUIT_STATES.clear();
        currentTimeMs = timeSupplier;
    }

    static String providerKeyForTest(String url) {
        return providerKey(url);
    }

    private static Request.Builder defaultGetBuilder(String url) {
        return defaultRequestBuilder(url)
                .get()
                .header(
                        "Accept",
                        "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
    }

    private static Request.Builder defaultRequestBuilder(String url) {
        return new Request.Builder()
                .url(url)
                .header("User-Agent", DEFAULT_USER_AGENT)
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
    }

    private static void addHeaders(Request.Builder builder, Map<String, String> headers) {
        if (headers != null) {
            headers.forEach(builder::header);
        }
    }

    private static byte[] executeBytes(
            Request request,
            String url,
            CrawlerRequestPolicy policy) throws IOException {
        String provider = providerKey(url);
        CircuitPermit permit = acquireCircuitPermit(provider);
        try {
            byte[] body = executeBytesWithRetries(request, url, policy);
            recordCircuitSuccess(provider);
            return body;
        } catch (ProviderRateLimitException | NonRetryableHttpException exception) {
            recordCircuitSuccess(provider);
            throw exception;
        } catch (ProviderCallFailureException exception) {
            recordCircuitFailure(provider, permit);
            throw exception;
        } catch (IOException exception) {
            releaseHalfOpenPermit(provider, permit);
            throw exception;
        } catch (RuntimeException exception) {
            releaseHalfOpenPermit(provider, permit);
            throw exception;
        }
    }

    private static byte[] executeBytesWithRetries(
            Request request,
            String url,
            CrawlerRequestPolicy policy) throws IOException {
        long deadlineNanos = System.nanoTime() + policy.callDeadline().toNanos();
        ProviderCallFailureException lastException = null;
        boolean eastMoneyRequest = isEastMoneyUrl(url);

        for (int attempt = 1; attempt <= policy.maxAttempts(); attempt++) {
            checkCooldown(url);
            throttle(url, eastMoneyRequest, deadlineNanos);
            Duration remaining = remaining(deadlineNanos, url);
            try (Response response = clientFor(policy, remaining).newCall(request).execute()) {
                int status = response.code();
                if (status == 429) {
                    Duration cooldown = retryAfter(response.header("Retry-After"));
                    startCooldown(url, cooldown);
                    throw new ProviderRateLimitException(providerKey(url), cooldown);
                }
                if (status >= 400 && status < 500) {
                    throw new NonRetryableHttpException("HTTP " + status + " from: " + url);
                }
                if (status >= 500) {
                    lastException = new ProviderCallFailureException(
                            "HTTP " + status + " from: " + url);
                    log.warn(
                            "crawler_http_server_error status={} attempt={}/{} url={}",
                            status,
                            attempt,
                            policy.maxAttempts(),
                            url);
                } else if (!response.isSuccessful()) {
                    throw new NonRetryableHttpException("HTTP " + status + " from: " + url);
                } else {
                    if (response.body() == null) {
                        throw new NonRetryableHttpException("Response body is null from: " + url);
                    }
                    return response.body().bytes();
                }
            } catch (ProviderRateLimitException | NonRetryableHttpException exception) {
                throw exception;
            } catch (IOException exception) {
                lastException = new ProviderCallFailureException(
                        "Network error [attempt " + attempt + "/" + policy.maxAttempts()
                                + "]: " + url,
                        exception);
                log.warn(
                        "crawler_http_network_error attempt={}/{} url={} message={}",
                        attempt,
                        policy.maxAttempts(),
                        url,
                        exception.getMessage());
            }

            if (attempt < policy.maxAttempts()) {
                waitBeforeRetry(attempt, deadlineNanos, url);
            }
        }

        throw lastException != null
                ? lastException
                : new IOException(
                        "Request failed after " + policy.maxAttempts() + " attempts: " + url);
    }

    private static OkHttpClient clientFor(
            CrawlerRequestPolicy policy,
            Duration remaining) {
        Duration connectTimeout = min(policy.connectTimeout(), remaining);
        Duration readTimeout = min(policy.readTimeout(), remaining);
        return CLIENT.newBuilder()
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .writeTimeout(readTimeout)
                .callTimeout(remaining)
                .retryOnConnectionFailure(false)
                .build();
    }

    private static Duration remaining(long deadlineNanos, String url) throws IOException {
        long remainingNanos = deadlineNanos - System.nanoTime();
        if (remainingNanos <= 0L) {
            throw new IOException("Request deadline exceeded: " + url);
        }
        return Duration.ofNanos(remainingNanos);
    }

    private static Duration min(Duration first, Duration second) {
        return first.compareTo(second) <= 0 ? first : second;
    }

    private static void waitBeforeRetry(int attempt, long deadlineNanos, String url)
            throws IOException {
        long baseDelayMs = RETRY_DELAY_MS * (1L << (attempt - 1));
        long jitterMs = ThreadLocalRandom.current().nextLong(0L, 101L);
        long delayMs = baseDelayMs + jitterMs;
        long remainingMs = Duration.ofNanos(Math.max(0L, deadlineNanos - System.nanoTime()))
                .toMillis();
        if (delayMs >= remainingMs) {
            throw new IOException("Request deadline exceeded before retry: " + url);
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted before retry: " + url, exception);
        }
    }

    private static void throttle(
            String url,
            boolean eastMoneyRequest,
            long deadlineNanos) throws IOException {
        if (eastMoneyRequest) {
            throttleEastMoney(deadlineNanos);
        } else {
            RateLimiter.throttleUntil(url, deadlineNanos);
        }
    }

    private static void throttleEastMoney(long deadlineNanos) throws IOException {
        synchronized (EASTMONEY_LOCK) {
            long remainingNanos = deadlineNanos - System.nanoTime();
            if (remainingNanos <= 0L) {
                throw new IOException(
                        "Request deadline exhausted while waiting for EastMoney throttle");
            }
            long jitterMs = ThreadLocalRandom.current().nextLong(100L, 501L);
            long targetIntervalMs = EASTMONEY_MIN_INTERVAL_MS + jitterMs;
            long elapsedMs = System.currentTimeMillis() - eastMoneyLastCallAtMs;
            if (elapsedMs < targetIntervalMs) {
                long sleepMs = targetIntervalMs - elapsedMs;
                if (sleepMs >= Duration.ofNanos(remainingNanos).toMillis()) {
                    throw new IOException(
                            "Request deadline exhausted while throttling EastMoney");
                }
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IOException(
                            "EastMoney request interrupted while throttling", exception);
                }
            }
            eastMoneyLastCallAtMs = System.currentTimeMillis();
        }
    }

    private static void checkCooldown(String url) throws IOException {
        String provider = providerKey(url);
        Long cooldownUntil = COOLDOWN_UNTIL_MS.get(provider);
        if (cooldownUntil == null) {
            return;
        }
        long remainingMs = cooldownUntil - currentTimeMs.getAsLong();
        if (remainingMs <= 0L) {
            COOLDOWN_UNTIL_MS.remove(provider, cooldownUntil);
            return;
        }
        throw new ProviderRateLimitException(provider, Duration.ofMillis(remainingMs));
    }

    private static void startCooldown(String url, Duration cooldown) {
        long until = currentTimeMs.getAsLong() + Math.max(0L, cooldown.toMillis());
        COOLDOWN_UNTIL_MS.merge(providerKey(url), until, Math::max);
    }

    private static Duration retryAfter(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_RATE_LIMIT_COOLDOWN;
        }
        try {
            return Duration.ofSeconds(Math.max(0L, Long.parseLong(value.trim())));
        } catch (NumberFormatException ignored) {
            try {
                Instant retryAt = ZonedDateTime.parse(
                                value.trim(), DateTimeFormatter.RFC_1123_DATE_TIME)
                        .toInstant();
                Duration duration = Duration.between(
                        Instant.ofEpochMilli(currentTimeMs.getAsLong()), retryAt);
                return duration.isNegative() ? Duration.ZERO : duration;
            } catch (DateTimeParseException ignoredAgain) {
                return DEFAULT_RATE_LIMIT_COOLDOWN;
            }
        }
    }

    private static String providerKey(String url) {
        try {
            String host = URI.create(url).getHost();
            if (host == null) {
                return "unknown";
            }
            String normalizedHost = host.toLowerCase();
            if (normalizedHost.equals("eastmoney.com")
                    || normalizedHost.endsWith(".eastmoney.com")
                    || normalizedHost.equals("dfcfw.com")
                    || normalizedHost.endsWith(".dfcfw.com")) {
                return "eastmoney";
            }
            return normalizedHost;
        } catch (IllegalArgumentException exception) {
            return "unknown";
        }
    }

    private static boolean isEastMoneyUrl(String url) {
        return "eastmoney".equals(providerKey(url));
    }

    private static CircuitPermit acquireCircuitPermit(String provider)
            throws ProviderCircuitOpenException {
        CircuitState state = CIRCUIT_STATES.computeIfAbsent(provider, ignored -> new CircuitState());
        synchronized (state) {
            long now = currentTimeMs.getAsLong();
            if (state.openUntilMs > now) {
                throw new ProviderCircuitOpenException(
                        provider, Duration.ofMillis(state.openUntilMs - now));
            }
            if (state.openUntilMs > 0L) {
                if (state.halfOpenProbeInFlight) {
                    throw new ProviderCircuitOpenException(provider, Duration.ofSeconds(1));
                }
                state.halfOpenProbeInFlight = true;
                return CircuitPermit.HALF_OPEN;
            }
            return CircuitPermit.CLOSED;
        }
    }

    private static void recordCircuitSuccess(String provider) {
        CircuitState state = CIRCUIT_STATES.get(provider);
        if (state == null) {
            return;
        }
        synchronized (state) {
            state.consecutiveFailures = 0;
            state.openUntilMs = 0L;
            state.halfOpenProbeInFlight = false;
        }
        CIRCUIT_STATES.remove(provider, state);
    }

    private static void recordCircuitFailure(String provider, CircuitPermit permit) {
        CircuitState state = CIRCUIT_STATES.computeIfAbsent(provider, ignored -> new CircuitState());
        synchronized (state) {
            if (permit == CircuitPermit.HALF_OPEN) {
                openCircuit(provider, state);
                return;
            }
            state.consecutiveFailures++;
            if (state.consecutiveFailures >= CIRCUIT_FAILURE_THRESHOLD) {
                openCircuit(provider, state);
            }
        }
    }

    private static void releaseHalfOpenPermit(String provider, CircuitPermit permit) {
        if (permit != CircuitPermit.HALF_OPEN) {
            return;
        }
        CircuitState state = CIRCUIT_STATES.get(provider);
        if (state != null) {
            synchronized (state) {
                state.halfOpenProbeInFlight = false;
            }
        }
    }

    private static void openCircuit(String provider, CircuitState state) {
        state.consecutiveFailures = CIRCUIT_FAILURE_THRESHOLD;
        state.openUntilMs = currentTimeMs.getAsLong() + CIRCUIT_OPEN_DURATION.toMillis();
        state.halfOpenProbeInFlight = false;
        log.warn("crawler_http_circuit_open provider={} duration={}",
                provider, CIRCUIT_OPEN_DURATION);
    }

    private enum CircuitPermit {
        CLOSED,
        HALF_OPEN
    }

    private static final class CircuitState {
        private int consecutiveFailures;
        private long openUntilMs;
        private boolean halfOpenProbeInFlight;
    }

    private static final class NonRetryableHttpException extends IOException {

        private NonRetryableHttpException(String message) {
            super(message);
        }
    }

    private static final class ProviderCallFailureException extends IOException {

        private ProviderCallFailureException(String message) {
            super(message);
        }

        private ProviderCallFailureException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
