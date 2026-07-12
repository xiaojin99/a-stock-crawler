package com.stock.crawler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stock.crawler.exception.ProviderRateLimitException;
import com.stock.crawler.model.MarketNewsItem;
import com.stock.crawler.util.CrawlerRequestPolicy;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class MarketNewsSourceTest {

    @Test
    void fetchNews_routesClsThroughControlledHttpAndAppliesLimit() {
        RecordingNewsHttpClient httpClient = new RecordingNewsHttpClient("""
                {"data":{"roll_data":[
                  {"id":1,"title":"first","content":"one","ctime":1710000000},
                  {"id":2,"title":"second","content":"two","ctime":1710000001}
                ]}}
                """);
        MarketNewsService service = new MarketNewsService(httpClient, new StubStockIntelligenceService());

        List<MarketNewsItem> result = service.fetchNews(MarketNewsSource.CLS, 1);

        assertEquals(1, result.size());
        assertEquals("first", result.getFirst().getTitle());
        assertEquals(1, httpClient.urls.size());
        assertTrue(httpClient.urls.getFirst().contains("rn=1"));
        assertEquals(CrawlerRequestPolicy.backgroundNews(), httpClient.policies.getFirst());
    }

    @Test
    void fetchNews_routesBaiduThroughControlledHttp() {
        RecordingNewsHttpClient httpClient = new RecordingNewsHttpClient("""
                {"Result":{"content":{"list":[
                  {"title":"baidu","publish_time":1710000000,"loc":"https://example.test/1",
                   "content":{"items":[{"data":"body"}]}}
                ]}}}
                """);
        MarketNewsService service = new MarketNewsService(httpClient, new StubStockIntelligenceService());

        List<MarketNewsItem> result = service.fetchNews(MarketNewsSource.BAIDU_EXPRESS, 4);

        assertEquals("百度股市通", result.getFirst().getSource());
        assertEquals(1, httpClient.urls.size());
    }

    @Test
    void fetchNews_routesEastMoneyWithoutCallingClsOrBaiduClient() {
        RecordingNewsHttpClient httpClient = new RecordingNewsHttpClient("{}");
        MarketNewsService service = new MarketNewsService(httpClient, new StubStockIntelligenceService());

        List<MarketNewsItem> result = service.fetchNews(MarketNewsSource.EASTMONEY_724, 3);

        assertEquals("eastmoney", result.getFirst().getTitle());
        assertEquals(0, httpClient.urls.size());
    }

    @Test
    void cacheReaders_neverFetchUpstreamWhenCacheIsEmpty() {
        RecordingNewsHttpClient httpClient = new RecordingNewsHttpClient("{}");
        MarketNewsService service = new MarketNewsService(httpClient, new StubStockIntelligenceService());

        assertEquals(List.of(), service.listHotNews(null, 4));
        assertEquals(List.of(), service.listHotBoards(4));
        assertEquals(0, httpClient.urls.size());
    }

    @Test
    void refreshHotNewsCache_keepsLastSuccessfulSourceWhenRefreshIsEmpty() {
        AtomicBoolean upstreamEmpty = new AtomicBoolean();
        MarketNewsHttpClient httpClient = (url, headers, policy) -> {
            if (upstreamEmpty.get()) {
                return "{}";
            }
            if (url.contains("cls.cn")) {
                return """
                        {"data":{"roll_data":[
                          {"id":1,"title":"cls","content":"body","ctime":1710000000}
                        ]}}
                        """;
            }
            return """
                    {"Result":{"content":{"list":[
                      {"title":"baidu","publish_time":1710000000,"loc":"https://example.test/1",
                       "content":{"items":[{"data":"body"}]}}
                    ]}}}
                    """;
        };
        MarketNewsService service = new MarketNewsService(
                httpClient, new StubStockIntelligenceService());
        service.refreshHotNewsCache();
        upstreamEmpty.set(true);

        service.refreshHotNewsCache();

        assertEquals(3, service.listHotNews(null, 10).size());
    }

    @Test
    void fetchNews_propagatesRecognizableRateLimitForRedisCooldown() {
        MarketNewsHttpClient httpClient = (url, headers, policy) -> {
            throw new ProviderRateLimitException("cls.cn", Duration.ofMinutes(2));
        };
        MarketNewsService service = new MarketNewsService(
                httpClient, new StubStockIntelligenceService());

        MarketNewsRateLimitException exception = assertThrows(
                MarketNewsRateLimitException.class,
                () -> service.fetchNews(MarketNewsSource.CLS, 4));

        assertEquals("cls.cn", exception.getProvider());
        assertEquals(Duration.ofMinutes(2), exception.getRetryAfter());
    }

    private static final class RecordingNewsHttpClient implements MarketNewsHttpClient {

        private final String response;
        private final List<String> urls = new ArrayList<>();
        private final List<CrawlerRequestPolicy> policies = new ArrayList<>();

        private RecordingNewsHttpClient(String response) {
            this.response = response;
        }

        @Override
        public String get(
                String url,
                java.util.Map<String, String> headers,
                CrawlerRequestPolicy policy) {
            urls.add(url);
            policies.add(policy);
            return response;
        }
    }

    private static final class StubStockIntelligenceService extends StockIntelligenceService {

        @Override
        public List<MarketNewsItem> getGlobalNews(int pageSize) throws IOException {
            return List.of(MarketNewsItem.builder().title("eastmoney").source("东方财富").build());
        }
    }
}
