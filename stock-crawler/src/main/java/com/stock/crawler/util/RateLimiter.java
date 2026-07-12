package com.stock.crawler.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按域名的 HTTP 请求限流器
 * 针对不同数据源设置不同的最小请求间隔 + 随机抖动，防止触发 IP 封禁。
 *
 * <p>参考 a-stock-data 项目的限流阈值（2026-05 社区实测）：</p>
 * <ul>
 *   <li>东方财富：每 2 秒至多发起一次</li>
 *   <li>财联社、百度、微博：每 5 秒至多发起一次</li>
 *   <li>腾讯财经：每 500 毫秒至多发起一次</li>
 * </ul>
 */
public final class RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    /** 各域名的最小请求间隔（毫秒） */
    private static final Map<String, Long> DOMAIN_INTERVALS = Map.of(
            "eastmoney.com", 2000L,
            "cls.cn",         5000L,
            "weibo.com",      5000L,
            "baidu.com",       5000L,
            "szse.cn",         500L,
            "sinajs.cn",       1000L,
            "gtimg.cn",        500L
    );

    /** 未匹配到域名时的默认最小间隔 */
    private static final long DEFAULT_INTERVAL_MS = 800L;

    /** 抖动系数：实际等待 = 最小间隔 × (1 + jitter × random) */
    private static final double JITTER_FACTOR = 0.3;

    /** 每个域名最后一次请求的时间戳 */
    private static final ConcurrentHashMap<String, Long> lastCallTime = new ConcurrentHashMap<>();

    /** 每个域名的锁对象，用于串行化同域名的限速判断 */
    private static final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    private RateLimiter() {
    }

    /**
     * 对指定 URL 的请求进行限速。
     * 根据域名匹配最小间隔，不足则 sleep 等待。
     *
     * @param url 即将请求的 URL
     */
    public static void throttle(String url) throws IOException {
        throttleUntil(url, System.nanoTime() + Duration.ofDays(1).toNanos());
    }

    static void throttleUntil(String url, long deadlineNanos) throws IOException {
        String domain = extractDomain(url);
        long minInterval = DOMAIN_INTERVALS.getOrDefault(domain, DEFAULT_INTERVAL_MS);
        long jitter = (long) (minInterval * JITTER_FACTOR * Math.random());
        long targetInterval = minInterval + jitter;

        Object lock = locks.computeIfAbsent(domain, d -> new Object());
        synchronized (lock) {
            long remainingNanos = deadlineNanos - System.nanoTime();
            if (remainingNanos <= 0L) {
                throw new IOException(
                        "Request deadline exhausted while waiting for domain: " + domain);
            }
            long now = System.currentTimeMillis();
            Long last = lastCallTime.get(domain);
            if (last != null) {
                long elapsed = now - last;
                if (elapsed < targetInterval) {
                    long sleepMs = targetInterval - elapsed;
                    if (sleepMs >= Duration.ofNanos(remainingNanos).toMillis()) {
                        throw new IOException(
                                "Request deadline exhausted while throttling domain: " + domain);
                    }
                    log.debug("rate_limiter domain={} sleepMs={}", domain, sleepMs);
                    try {
                        Thread.sleep(sleepMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Rate limiter interrupted for domain: " + domain, e);
                    }
                }
            }
            lastCallTime.put(domain, System.currentTimeMillis());
        }
    }

    /**
     * 从 URL 中提取域名关键词用于匹配。
     * 例如 https://push2.eastmoney.com/api/... -> eastmoney.com
     */
    private static String extractDomain(String url) {
        if (url == null || url.isEmpty()) {
            return "unknown";
        }
        String host;
        try {
            host = URI.create(url).getHost();
        } catch (Exception e) {
            return "unknown";
        }
        if (host == null) {
            return "unknown";
        }
        host = host.toLowerCase();
        for (String known : DOMAIN_INTERVALS.keySet()) {
            if (host.contains(known)) {
                return known;
            }
        }
        return host;
    }
}
