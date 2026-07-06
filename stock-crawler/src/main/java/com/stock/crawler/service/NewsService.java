package com.stock.crawler.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.crawler.model.Telegraph;
import com.stock.crawler.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 财经快讯服务
 * 爬取财联社快讯数据
 */
public class NewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String CLS_TELEGRAPH_URL = "https://www.cls.cn/api/cache";
    private static final int DEFAULT_LIMIT = 20;
    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private List<Telegraph> cache = new ArrayList<>();
    private LocalDateTime lastFetchTime;
    private static final Duration CACHE_TTL = Duration.ofSeconds(30);

    /**
     * 获取财联社快讯列表
     * 使用 30 秒缓存，避免频繁请求
     */
    public List<Telegraph> getTelegraphList() throws IOException {
        lock.readLock().lock();
        try {
            if (isCacheFresh()) {
                return new ArrayList<>(cache);
            }
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            if (isCacheFresh()) {
                return new ArrayList<>(cache);
            }
            cache = fetchTelegraphList();
            lastFetchTime = LocalDateTime.now();
            return new ArrayList<>(cache);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取最新一条快讯
     */
    public Telegraph getLatestTelegraph() throws IOException {
        List<Telegraph> list = getTelegraphList();
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 从财联社爬取快讯数据
     */
    private List<Telegraph> fetchTelegraphList() throws IOException {
        log.info("Fetching telegraph list from cls.cn");

        Map<String, String> headers = Map.of(
                "Accept", "application/json, text/plain, */*",
                "Referer", "https://www.cls.cn/telegraph",
                "Accept-Language", "zh-CN,zh;q=0.9"
        );

        String url = CLS_TELEGRAPH_URL
                + "?rn=" + DEFAULT_LIMIT
                + "&lastTime=" + Instant.now().getEpochSecond()
                + "&name=telegraph";
        String json = HttpUtils.get(url, headers);
        JsonNode root = OBJECT_MAPPER.readTree(json);
        JsonNode contentBoxes = root.path("data").path("roll_data");

        List<Telegraph> telegraphs = new ArrayList<>();

        if (!contentBoxes.isArray() || contentBoxes.isEmpty()) {
            throw new IOException("No telegraph items found at " + url);
        }

        for (JsonNode item : contentBoxes) {
            String time = formatTime(item.path("ctime").asLong(0L));
            String content = firstNonBlank(
                    text(item, "content"),
                    text(item, "brief"),
                    text(item, "title"));
            content = cleanContent(content);
            String detailUrl = buildDetailUrl(item);

            if (!content.isEmpty()) {
                telegraphs.add(new Telegraph(time, content, detailUrl));
            }
        }

        log.info("Fetched {} telegraphs", telegraphs.size());
        return telegraphs;
    }

    /**
     * 清理内容中的多余空白字符
     */
    private String cleanContent(String s) {
        if (s == null) return "";
        // 替换多个空白字符为单个空格
        s = s.replaceAll("<[^>]+>", " ");
        s = s.replaceAll("\\s+", " ");
        s = s.replace("\n", " ");
        s = s.replace("\r", "");
        s = s.replace("\t", " ");
        return s.trim();
    }

    private String formatTime(long epochSeconds) {
        if (epochSeconds <= 0L) {
            return "";
        }
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), CHINA_ZONE)
                .format(TIME_FORMATTER);
    }

    private String buildDetailUrl(JsonNode item) {
        String shareUrl = text(item, "shareurl");
        if (!shareUrl.isBlank()) {
            return shareUrl;
        }
        long id = item.path("id").asLong(0L);
        return id > 0L ? "https://www.cls.cn/detail/" + id : "";
    }

    private String text(JsonNode parent, String fieldName) {
        JsonNode value = parent == null ? null : parent.get(fieldName);
        return value == null || value.isNull() ? "" : value.asText();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private boolean isCacheFresh() {
        return lastFetchTime != null
                && Duration.between(lastFetchTime, LocalDateTime.now()).compareTo(CACHE_TTL) < 0
                && !cache.isEmpty();
    }
}
