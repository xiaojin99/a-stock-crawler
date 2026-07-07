package com.stock.crawler.service;

import com.stock.crawler.fetcher.BaiduFetcher;
import com.stock.crawler.fetcher.Fetcher;
import com.stock.crawler.fetcher.WeiboFetcher;
import com.stock.crawler.model.HotItem;
import com.stock.crawler.model.HotTrendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 舆情热点聚合服务
 */
public class HotTrendService {

    private static final Logger log = LoggerFactory.getLogger(HotTrendService.class);

    private final Map<String, Fetcher> fetchers = new HashMap<>();
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final int MAX_CONCURRENT_FETCHERS = 4;

    private static class CacheEntry {
        final List<HotItem> items;
        final LocalDateTime timestamp;

        CacheEntry(List<HotItem> items) {
            this.items = items;
            this.timestamp = LocalDateTime.now();
        }

        boolean isExpired() {
            return Duration.between(timestamp, LocalDateTime.now()).compareTo(CACHE_TTL) > 0;
        }
    }

    public HotTrendService() {
        // 注册 fetcher
        registerFetcher(new WeiboFetcher());
        registerFetcher(new BaiduFetcher());
    }

    public void registerFetcher(Fetcher fetcher) {
        fetchers.put(fetcher.platform(), fetcher);
    }

    /**
     * 获取支持的平台列表
     */
    public List<PlatformInfo> getPlatforms() {
        return fetchers.values().stream()
                .map(f -> new PlatformInfo(f.platform(), f.platformCN()))
                .toList();
    }

    /**
     * 平台信息
     */
    public record PlatformInfo(String id, String name) {
    }

    /**
     * 获取单个平台的热点数据
     */
    public HotTrendResult getHotTrend(String platform) {
        Fetcher fetcher = fetchers.get(platform);
        if (fetcher == null) {
            return HotTrendResult.error(platform, platform, "不支持的平台");
        }

        // 检查缓存
        CacheEntry cached = cache.get(platform);
        if (cached != null && !cached.isExpired()) {
            HotTrendResult result = new HotTrendResult(platform, fetcher.platformCN(), cached.items);
            result.setFromCache(true);
            return result;
        }

        // 从网络获取
        try {
            List<HotItem> items = fetcher.fetch();
            cache.put(platform, new CacheEntry(items));

            HotTrendResult result = new HotTrendResult(platform, fetcher.platformCN(), items);
            result.setFromCache(false);
            return result;
        } catch (IOException e) {
            log.error("Failed to fetch hot trend for platform: {}", platform, e);
            return HotTrendResult.error(platform, fetcher.platformCN(), e.getMessage());
        }
    }

    /**
     * 并发获取所有平台的热点数据
     */
    public List<HotTrendResult> getAllHotTrends() {
        return getHotTrends(new ArrayList<>(fetchers.keySet()));
    }

    /**
     * 并发获取指定平台的热点数据
     */
    public List<HotTrendResult> getHotTrends(List<String> platforms) {
        List<String> requestedPlatforms = normalizeRequestedPlatforms(platforms);
        List<HotTrendResult> results = new ArrayList<>();
        if (requestedPlatforms.isEmpty()) {
            return results;
        }

        int poolSize = Math.min(MAX_CONCURRENT_FETCHERS, requestedPlatforms.size());
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                poolSize,
                poolSize,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(requestedPlatforms.size()),
                hotTrendThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());
        try {
            List<Callable<HotTrendResult>> tasks = requestedPlatforms.stream()
                    .<Callable<HotTrendResult>>map(platform -> () -> getHotTrend(platform))
                    .toList();
            List<Future<HotTrendResult>> futures = executor.invokeAll(tasks, 30, TimeUnit.SECONDS);
            for (Future<HotTrendResult> future : futures) {
                if (future.isCancelled()) {
                    continue;
                }
                try {
                    results.add(future.get());
                } catch (ExecutionException ex) {
                    log.warn("hot_trend_task_failed message={}", ex.getMessage(), ex);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while fetching hot trends", e);
        } finally {
            executor.shutdownNow();
        }

        // 若所有平台均失败，输出汇总日志
        long failCount = results.stream().filter(HotTrendResult::hasError).count();
        if (failCount == results.size() && !results.isEmpty()) {
            log.error("All {} platforms failed to fetch hot trends", failCount);
            results.forEach(r -> log.error("  Platform [{}] error: {}", r.getPlatform(), r.getError()));
        } else if (failCount > 0) {
            log.warn("{}/{} platforms failed to fetch hot trends", failCount, results.size());
        }

        return results;
    }

    private List<String> normalizeRequestedPlatforms(List<String> platforms) {
        if (platforms == null || platforms.isEmpty()) {
            return List.of();
        }
        Set<String> uniquePlatforms = new LinkedHashSet<>();
        for (String platform : platforms) {
            if (platform == null || platform.isBlank()) {
                continue;
            }
            String normalized = platform.trim();
            if (fetchers.containsKey(normalized)) {
                uniquePlatforms.add(normalized);
            }
        }
        return new ArrayList<>(uniquePlatforms);
    }

    private ThreadFactory hotTrendThreadFactory() {
        AtomicInteger sequence = new AtomicInteger(1);
        return runnable -> {
            Thread thread = new Thread(runnable, "hot-trend-fetch-" + sequence.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }
}
