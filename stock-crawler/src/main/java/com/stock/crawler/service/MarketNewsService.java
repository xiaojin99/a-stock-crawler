package com.stock.crawler.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.crawler.exception.ProviderRateLimitException;
import com.stock.crawler.model.MarketHotBoard;
import com.stock.crawler.model.MarketHotItem;
import com.stock.crawler.model.MarketNewsItem;
import com.stock.crawler.util.CrawlerRequestPolicy;
import com.stock.crawler.util.HttpUtils;
import com.stock.crawler.util.StockCodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 市场快讯服务
 * 聚合财联社、百度股市通多源新闻
 * 注意：本类不含 Spring 调度，请在上层（如 Spring 包装类）中定期调用 refreshHotNewsCache()
 */
public class MarketNewsService {

    private static final Logger log = LoggerFactory.getLogger(MarketNewsService.class);

    private static final String CLS_NEWS_URL = "https://www.cls.cn/api/cache";
    private static final String BAIDU_EXPRESS_NEWS_URL = "https://finance.pae.baidu.com/selfselect/expressnews";
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;
    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter NEWS_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectMapper objectMapper;
    private final MarketNewsHttpClient httpClient;
    private final StockIntelligenceService stockIntelligenceService;

    private volatile List<MarketNewsItem> cachedHotNews = new ArrayList<>();
    private volatile List<MarketHotBoard> cachedHotBoards = new ArrayList<>();
    private volatile Map<MarketNewsSource, List<MarketNewsItem>> cachedNewsBySource = Map.of();

    public MarketNewsService() {
        this(HttpUtils::get, new StockIntelligenceService());
    }

    MarketNewsService(
            MarketNewsHttpClient httpClient,
            StockIntelligenceService stockIntelligenceService) {
        this.objectMapper = new ObjectMapper();
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.stockIntelligenceService = Objects.requireNonNull(
                stockIntelligenceService, "stockIntelligenceService");
    }

    /**
     * 刷新热点新闻缓存（应由外部定期调用）
     */
    public synchronized void refreshHotNewsCache() {
        EnumMap<MarketNewsSource, List<MarketNewsItem>> refreshed =
                new EnumMap<>(MarketNewsSource.class);
        refreshed.putAll(cachedNewsBySource);
        refreshSource(refreshed, MarketNewsSource.EASTMONEY_724);
        refreshSource(refreshed, MarketNewsSource.CLS);
        refreshSource(refreshed, MarketNewsSource.BAIDU_EXPRESS);
        cachedNewsBySource = Map.copyOf(refreshed);

        List<MarketNewsItem> eastMoneyNews = sourceSnapshot(
                refreshed, MarketNewsSource.EASTMONEY_724);
        List<MarketNewsItem> clsNews = sourceSnapshot(refreshed, MarketNewsSource.CLS);
        List<MarketNewsItem> baiduNews = sourceSnapshot(
                refreshed, MarketNewsSource.BAIDU_EXPRESS);
        List<MarketNewsItem> merged = mergeNews(eastMoneyNews, baiduNews, clsNews);
        cachedHotNews = List.copyOf(merged);
        cachedHotBoards = buildBoards(eastMoneyNews, baiduNews, clsNews);
        log.info("market_news_cache_refreshed mergedSize={} boardSize={}", merged.size(), cachedHotBoards.size());
    }

    /** Fetches one provider independently so callers can persist source-specific snapshots. */
    public List<MarketNewsItem> fetchNews(MarketNewsSource source, int limit) {
        Objects.requireNonNull(source, "source");
        int safeLimit = StockCodeUtils.clamp(limit, 1, MAX_LIMIT);
        return switch (source) {
            case EASTMONEY_724 -> fetchEastMoneyGlobalNews(safeLimit);
            case CLS -> fetchClsNews(safeLimit);
            case BAIDU_EXPRESS -> fetchBaiduNews(safeLimit);
        };
    }

    private void refreshSource(
            EnumMap<MarketNewsSource, List<MarketNewsItem>> snapshots,
            MarketNewsSource source) {
        List<MarketNewsItem> fetched = fetchNews(source, MAX_LIMIT);
        if (!fetched.isEmpty()) {
            snapshots.put(source, List.copyOf(fetched));
        } else if (snapshots.containsKey(source)) {
            log.warn("market_news_refresh_kept_stale source={}", source);
        }
    }

    private List<MarketNewsItem> sourceSnapshot(
            Map<MarketNewsSource, List<MarketNewsItem>> snapshots,
            MarketNewsSource source) {
        return snapshots.getOrDefault(source, List.of());
    }

    /**
     * 获取热点新闻列表（支持关键词过滤）
     */
    public List<MarketNewsItem> listHotNews(String keyword, Integer limit) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        int safeLimit = sanitizeLimit(limit);
        List<MarketNewsItem> source = cachedHotNews;
        List<MarketNewsItem> filtered = new ArrayList<>();
        for (MarketNewsItem item : source) {
            String title = item.getTitle() == null ? "" : item.getTitle();
            String content = item.getContent() == null ? "" : item.getContent();
            if (!normalizedKeyword.isEmpty()
                    && !title.contains(normalizedKeyword)
                    && !content.contains(normalizedKeyword)) {
                continue;
            }
            filtered.add(item);
            if (filtered.size() >= safeLimit) {
                break;
            }
        }
        return filtered;
    }

    /**
     * 获取热榜看板（多源）
     */
    public List<MarketHotBoard> listHotBoards(Integer limit) {
        int safeLimit = sanitizeLimit(limit);
        List<MarketHotBoard> boards = cachedHotBoards;
        List<MarketHotBoard> result = new ArrayList<>();
        for (MarketHotBoard board : boards) {
            List<MarketHotItem> items = board.getItems() == null ? new ArrayList<>() : board.getItems();
            List<MarketHotItem> limited = new ArrayList<>();
            for (int i = 0; i < items.size() && i < safeLimit; i++) {
                MarketHotItem item = items.get(i);
                limited.add(MarketHotItem.builder()
                        .rank(i + 1)
                        .title(item.getTitle())
                        .subtitle(item.getSubtitle())
                        .time(item.getTime())
                        .heat(item.getHeat())
                        .url(item.getUrl())
                        .build());
            }
            result.add(MarketHotBoard.builder()
                    .boardId(board.getBoardId())
                    .boardName(board.getBoardName())
                    .source(board.getSource())
                    .updateTime(board.getUpdateTime())
                    .items(limited)
                    .build());
        }
        return result;
    }

    private int sanitizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    @SafeVarargs
    private final List<MarketNewsItem> mergeNews(List<MarketNewsItem>... sources) {
        Map<String, MarketNewsItem> deduplicated = new LinkedHashMap<>();
        for (List<MarketNewsItem> source : sources) {
            if (source == null) {
                continue;
            }
            for (MarketNewsItem item : source) {
                deduplicated.putIfAbsent(buildDedupKey(item), item);
            }
        }
        List<MarketNewsItem> merged = new ArrayList<>(deduplicated.values());
        merged.sort(Comparator.comparingLong(this::newsEpochMillis).reversed());
        return merged;
    }

    private String buildDedupKey(MarketNewsItem item) {
        return (item.getTitle() == null ? "" : item.getTitle().trim())
                + "#"
                + (item.getTime() == null ? "" : item.getTime().trim());
    }

    private List<MarketNewsItem> fetchClsNews(int limit) {
        try {
            String body = httpClient.get(
                    buildClsNewsUrl(limit),
                    Map.of(
                            "Referer", "https://www.cls.cn/telegraph",
                            "Accept", "application/json, text/plain, */*"),
                    CrawlerRequestPolicy.backgroundNews());
            JsonNode root = objectMapper.readTree(body);
            JsonNode list = root.path("data").path("roll_data");
            if (!list.isArray()) {
                return new ArrayList<>();
            }
            List<MarketNewsItem> results = new ArrayList<>();
            for (JsonNode item : list) {
                String content = cleanText(firstNonBlank(
                        text(item, "content"),
                        text(item, "brief")));
                String title = cleanText(firstNonBlank(
                        text(item, "title"),
                        text(item, "brief"),
                        content));
                results.add(MarketNewsItem.builder()
                        .title(title)
                        .content(content)
                        .time(formatEpochSeconds(item.path("ctime").asLong(0L)))
                        .url(buildClsDetailUrl(item))
                        .source("财联社")
                        .build());
                if (results.size() >= limit) {
                    break;
                }
            }
            return results;
        } catch (ProviderRateLimitException ex) {
            throw new MarketNewsRateLimitException(ex);
        } catch (IOException ex) {
            log.warn("market_news_fetch_failed source=cls type=io message={}", ex.getMessage(), ex);
            return new ArrayList<>();
        } catch (RuntimeException ex) {
            log.warn("market_news_fetch_failed source=cls message={}", ex.getMessage(), ex);
            return new ArrayList<>();
        }
    }

    private List<MarketNewsItem> fetchEastMoneyGlobalNews(int limit) {
        try {
            return stockIntelligenceService.getGlobalNews(limit);
        } catch (ProviderRateLimitException ex) {
            throw new MarketNewsRateLimitException(ex);
        } catch (Exception ex) {
            log.warn("market_news_fetch_failed source=eastmoney_724 message={}", ex.getMessage());
            return new ArrayList<>();
        }
    }

    private List<MarketNewsItem> fetchBaiduNews(int limit) {
        try {
            String body = httpClient.get(
                    BAIDU_EXPRESS_NEWS_URL,
                    Map.of(
                            "Referer", "https://gushitong.baidu.com/",
                            "Accept", "application/json, text/plain, */*"),
                    CrawlerRequestPolicy.backgroundNews());
            JsonNode root = objectMapper.readTree(body);
            // 尝试大小写两种路径（接口偏移当化）
            JsonNode list = root.path("Result").path("content").path("list");
            if (!list.isArray()) {
                list = root.path("result").path("content").path("list");
            }
            if (!list.isArray()) {
                log.debug("market_news_baidu_no_list body_prefix={}",
                        body.length() > 300 ? body.substring(0, 300) : body);
                return new ArrayList<>();
            }
            List<MarketNewsItem> results = new ArrayList<>();
            for (JsonNode item : list) {
                String title = text(item, "title");
                String content = flattenBaiduContent(item.path("content").path("items"));
                long publishTime = item.path("publish_time").asLong(0L);
                String normalizedTime = publishTime > 0
                        ? formatNewsTime(Instant.ofEpochSecond(publishTime))
                        : "";
                results.add(MarketNewsItem.builder()
                        .title(title)
                        .content(content)
                        .time(normalizedTime)
                        .url(text(item, "loc"))
                        .source("百度股市通")
                        .build());
                if (results.size() >= limit) {
                    break;
                }
            }
            return results;
        } catch (ProviderRateLimitException ex) {
            throw new MarketNewsRateLimitException(ex);
        } catch (IOException ex) {
            log.warn("market_news_fetch_failed source=baidu type=io message={}", ex.getMessage(), ex);
            return new ArrayList<>();
        } catch (RuntimeException ex) {
            log.warn("market_news_fetch_failed source=baidu message={}", ex.getMessage(), ex);
            return new ArrayList<>();
        }
    }

    private List<MarketHotBoard> buildBoards(
            List<MarketNewsItem> eastMoneyNews,
            List<MarketNewsItem> baiduNews,
            List<MarketNewsItem> clsNews) {
        List<MarketHotBoard> boards = new ArrayList<>();
        boards.add(newsToBoard("eastmoney_724", "东方财富7x24", eastMoneyNews));
        boards.add(newsToBoard("baidu", "百度股市通", baiduNews));
        boards.add(newsToBoard("cls", "财联社", clsNews));
        return boards;
    }

    private MarketHotBoard newsToBoard(String boardId, String boardName, List<MarketNewsItem> news) {
        List<MarketHotItem> items = new ArrayList<>();
        List<MarketNewsItem> source = news == null ? new ArrayList<>() : news;
        for (int i = 0; i < source.size() && i < 30; i++) {
            MarketNewsItem item = source.get(i);
            String content = item.getContent() == null ? "" : item.getContent().trim();
            items.add(MarketHotItem.builder()
                    .rank(i + 1)
                    .title(item.getTitle())
                    .subtitle(content.length() > 48 ? content.substring(0, 48) + "..." : content)
                    .time(item.getTime())
                    .heat(String.valueOf(Math.max(1000 - i * 17, 120)))
                    .url(item.getUrl())
                    .build());
        }
        return MarketHotBoard.builder()
                .boardId(boardId)
                .boardName(boardName)
                .source(boardName)
                .updateTime(LocalDateTime.now().toString())
                .items(items)
                .build();
    }

    private String flattenBaiduContent(JsonNode items) {
        if (!items.isArray()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode node : items) {
            String data = text(node, "data");
            if (data.isBlank()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            sb.append(data.trim());
        }
        return sb.toString();
    }

    private String buildClsNewsUrl(int limit) {
        return CLS_NEWS_URL
                + "?rn=" + limit
                + "&lastTime=" + Instant.now().getEpochSecond()
                + "&name=telegraph";
    }

    private String buildClsDetailUrl(JsonNode item) {
        String shareUrl = text(item, "shareurl");
        if (!shareUrl.isBlank()) {
            return shareUrl;
        }
        long id = item.path("id").asLong(0L);
        return id > 0L ? "https://www.cls.cn/detail/" + id : "";
    }

    private String formatEpochSeconds(long epochSeconds) {
        if (epochSeconds <= 0L) {
            return "";
        }
        return formatNewsTime(Instant.ofEpochSecond(epochSeconds));
    }

    private String formatNewsTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, CHINA_ZONE).format(NEWS_TIME_FORMATTER);
    }

    private long newsEpochMillis(MarketNewsItem item) {
        if (item == null || item.getTime() == null || item.getTime().isBlank()) {
            return 0L;
        }
        String time = item.getTime().trim();
        try {
            return LocalDateTime.parse(time, NEWS_TIME_FORMATTER)
                    .atZone(CHINA_ZONE)
                    .toInstant()
                    .toEpochMilli();
        } catch (DateTimeParseException ignored) {
            // Compatibility with older cached/test values that used LocalDateTime.toString().
            try {
                return LocalDateTime.parse(time)
                        .atZone(CHINA_ZONE)
                        .toInstant()
                        .toEpochMilli();
            } catch (DateTimeParseException ignoredAgain) {
                return 0L;
            }
        }
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

    private String cleanText(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String text(JsonNode parent, String fieldName) {
        if (parent == null) {
            return "";
        }
        JsonNode node = parent.path(fieldName);
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        String value = node.asText();
        return value == null ? "" : value;
    }
}
