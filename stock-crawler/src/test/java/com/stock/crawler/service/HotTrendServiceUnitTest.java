package com.stock.crawler.service;

import com.stock.crawler.fetcher.Fetcher;
import com.stock.crawler.model.HotItem;
import com.stock.crawler.model.HotTrendResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HotTrendServiceUnitTest {

    @Test
    void emptyPlatformListReturnsEmptyResult() {
        HotTrendService service = new HotTrendService();

        List<HotTrendResult> results = service.getHotTrends(List.of());

        assertTrue(results.isEmpty());
    }

    @Test
    void duplicateAndUnsupportedPlatformsAreFilteredBeforeFetching() {
        HotTrendService service = new HotTrendService();
        CountingFetcher fetcher = new CountingFetcher();
        service.registerFetcher(fetcher);

        List<HotTrendResult> results = service.getHotTrends(
                Arrays.asList("unit", "missing", "unit", "", null));

        assertEquals(1, results.size());
        assertEquals("unit", results.get(0).getPlatform());
        assertEquals(1, fetcher.callCount.get());
    }

    private static class CountingFetcher implements Fetcher {
        private final AtomicInteger callCount = new AtomicInteger();

        @Override
        public String platform() {
            return "unit";
        }

        @Override
        public String platformCN() {
            return "单元测试";
        }

        @Override
        public List<HotItem> fetch() throws IOException {
            callCount.incrementAndGet();
            return List.of(new HotItem("unit_1", "测试热点", "https://example.com", 1, "unit"));
        }
    }
}
