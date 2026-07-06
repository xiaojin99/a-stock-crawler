package com.stock.crawler.util;

import okhttp3.MediaType;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * HTTP 工具类，封装 OkHttp 客户端
 * 支持自动重试：网络超时和 5xx 错误最多重试 3 次，指数退避
 */
public class HttpUtils {

    private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(15))
            .writeTimeout(Duration.ofSeconds(10))
            .followRedirects(true)
            .build();

    private static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";

    /** GBK 字符集：腾讯财经等接口的响应编码 */
    public static final Charset GBK = Charset.forName("GBK");

    /** 最大重试次数 */
    private static final int MAX_RETRIES = 3;
    /** 初始重试延迟（毫秒），每次加倍 */
    private static final long RETRY_DELAY_MS = 1000L;
    /** 东方财富接口统一串行限流，避免 push2/datacenter/reportapi 高频风控 */
    private static final Object EASTMONEY_LOCK = new Object();
    private static final long EASTMONEY_MIN_INTERVAL_MS =
            Long.getLong("stockcrawler.eastmoney.minIntervalMs", 1200L);
    private static volatile long eastMoneyLastCallAtMs = 0L;

    private HttpUtils() {
    }

    /**
     * 执行 GET 请求，返回响应体字符串
     */
    public static String get(String url) throws IOException {
        return get(url, null);
    }

    /**
     * 执行 GET 请求，支持自定义请求头
     */
    public static String get(String url, Map<String, String> headers) throws IOException {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .header("User-Agent", DEFAULT_USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");

        if (headers != null) {
            headers.forEach(builder::header);
        }

        return executeWithRetry(builder.build(), url);
    }

    /**
     * 东方财富统一 GET 入口。
     *
     * <p>所有 eastmoney.com / dfcfw.com 端点都应走这里：串行限流、浏览器请求头、
     * 共享 OkHttp 会话和网络异常重试，降低 push2/datacenter 风控和空响应影响。</p>
     */
    public static String getEastMoney(String url, Map<String, String> headers) throws IOException {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .header("User-Agent", DEFAULT_USER_AGENT)
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("Referer", "https://quote.eastmoney.com/");

        if (headers != null) {
            headers.forEach(builder::header);
        }

        return executeWithRetry(builder.build(), url, true);
    }

    /**
     * 执行 GET 请求，以指定字符集解码响应体
     * 用于腾讯财经等返回 GBK 编码的接口
     */
    public static String getWithCharset(String url, Charset charset) throws IOException {
        RateLimiter.throttle(url);
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", DEFAULT_USER_AGENT)
                .get()
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + " from: " + url);
            }
            if (response.body() == null) {
                throw new IOException("Response body is null from: " + url);
            }
            return new String(response.body().bytes(), charset);
        }
    }

    /**
     * 执行 GET 请求，返回字节数组（用于下载）
     */
    public static byte[] getBytes(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", DEFAULT_USER_AGENT)
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + " from: " + url);
            }
            if (response.body() == null) {
                throw new IOException("Response body is null from: " + url);
            }
            return response.body().bytes();
        }
    }

    /**
     * 获取共享的 OkHttpClient 实例
     */
    public static OkHttpClient getClient() {
        return CLIENT;
    }

    /**
     * 执行 POST 请求 (JSON Body)
     */
    public static String post(String url, String jsonBody) throws IOException {
        return post(url, jsonBody, null);
    }

    /**
     * 执行 POST 请求 (JSON Body)，支持自定义请求头
     */
    public static String post(String url, String jsonBody, Map<String, String> headers) throws IOException {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonBody, JSON);

        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(body)
                .header("User-Agent", DEFAULT_USER_AGENT)
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");

        if (headers != null) {
            headers.forEach(builder::header);
        }

        return executeWithRetry(builder.build(), url);
    }

    /**
     * 执行 POST 表单请求，适用于巨潮等 x-www-form-urlencoded 接口。
     */
    public static String postForm(String url, Map<String, String> form, Map<String, String> headers) throws IOException {
        FormBody.Builder formBuilder = new FormBody.Builder();
        if (form != null) {
            form.forEach((key, value) -> formBuilder.add(key, value == null ? "" : value));
        }

        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(formBuilder.build())
                .header("User-Agent", DEFAULT_USER_AGENT)
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");

        if (headers != null) {
            headers.forEach(builder::header);
        }

        return executeWithRetry(builder.build(), url);
    }

    /**
     * 带指数退避重试的 HTTP 执行逻辑。
     * <p>重试条件：网络超时 / 连接异常 / HTTP 5xx 服务端错误。</p>
     * <p>HTTP 4xx 客户端错误不重试，直接抛出。</p>
     *
     * @param request 已构建的 OkHttp 请求
     * @param url     请求 URL，用于日志和异常信息
     * @return 响应体字符串
     * @throws IOException 重试耗尽后仍失败
     */
    private static String executeWithRetry(Request request, String url) throws IOException {
        return executeWithRetry(request, url, isEastMoneyUrl(url));
    }

    private static String executeWithRetry(Request request, String url, boolean eastMoneyRequest) throws IOException {
        if (!eastMoneyRequest) {
            RateLimiter.throttle(url);
        }
        IOException lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            if (eastMoneyRequest) {
                throttleEastMoney();
            }
            try (Response response = CLIENT.newCall(request).execute()) {
                int code = response.code();

                // 4xx 客户端错误：直接抛出，不重试
                if (code >= 400 && code < 500) {
                    throw new IOException("HTTP " + code + " from: " + url);
                }

                // 5xx 服务端错误：记录并尝试重试
                if (code >= 500) {
                    lastException = new IOException("HTTP " + code + " from: " + url);
                    log.warn("Request failed with HTTP {} [attempt {}/{}]: {}", code, attempt, MAX_RETRIES, url);
                } else if (!response.isSuccessful()) {
                    lastException = new IOException("HTTP " + code + " from: " + url);
                    log.warn("Unexpected HTTP {} [attempt {}/{}]: {}", code, attempt, MAX_RETRIES, url);
                } else {
                    // 成功
                    if (response.body() == null) {
                        throw new IOException("Response body is null from: " + url);
                    }
                    return response.body().string();
                }
            } catch (SocketTimeoutException e) {
                lastException = new IOException("Request timed out [attempt " + attempt + "/" + MAX_RETRIES + "]: " + url, e);
                log.warn("Request timed out [attempt {}/{}]: {}", attempt, MAX_RETRIES, url);
            } catch (IOException e) {
                // 其他网络异常（连接拒绝等）也重试
                if (e.getMessage() != null && e.getMessage().startsWith("HTTP 4")) {
                    throw e; // 4xx 不重试
                }
                if (eastMoneyRequest) {
                    CLIENT.connectionPool().evictAll();
                }
                lastException = new IOException("Network error [attempt " + attempt + "/" + MAX_RETRIES + "]: " + url, e);
                log.warn("Network error [attempt {}/{}]: {} - {}", attempt, MAX_RETRIES, url, e.getMessage());
            }

            // 如果还有重试次数，等待指数退避时间
            if (attempt < MAX_RETRIES) {
                long delay = RETRY_DELAY_MS * (1L << (attempt - 1)); // 1s, 2s, 4s
                log.info("Retrying in {}ms...", delay);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted: " + url, ie);
                }
            }
        }

        log.error("All {} retries exhausted for: {}", MAX_RETRIES, url);
        throw lastException != null ? lastException
                : new IOException("Request failed after " + MAX_RETRIES + " retries: " + url);
    }

    private static void throttleEastMoney() throws IOException {
        synchronized (EASTMONEY_LOCK) {
            long jitterMs = ThreadLocalRandom.current().nextLong(100L, 501L);
            long targetIntervalMs = EASTMONEY_MIN_INTERVAL_MS + jitterMs;
            long elapsedMs = System.currentTimeMillis() - eastMoneyLastCallAtMs;
            if (elapsedMs < targetIntervalMs) {
                long sleepMs = targetIntervalMs - elapsedMs;
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IOException("EastMoney request interrupted while throttling", ex);
                }
            }
            eastMoneyLastCallAtMs = System.currentTimeMillis();
        }
    }

    private static boolean isEastMoneyUrl(String url) {
        if (url == null) {
            return false;
        }
        String lower = url.toLowerCase();
        return lower.contains("eastmoney.com") || lower.contains("dfcfw.com");
    }
}
