package com.stock.crawler.fetcher;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stock.crawler.util.CrawlerRequestPolicy;
import org.junit.jupiter.api.Test;

class HotTrendRequestPolicyTest {

    @Test
    void baiduFetcher_usesBackgroundNewsPolicy() throws Exception {
        HotTrendHttpClient httpClient = mock(HotTrendHttpClient.class);
        when(httpClient.get(anyString(), anyMap(), eq(CrawlerRequestPolicy.backgroundNews())))
                .thenReturn("{}");
        BaiduFetcher fetcher = new BaiduFetcher(httpClient);

        assertTrue(fetcher.fetch().isEmpty());

        verify(httpClient).get(anyString(), anyMap(), eq(CrawlerRequestPolicy.backgroundNews()));
    }

    @Test
    void weiboFetcher_usesBackgroundNewsPolicy() throws Exception {
        HotTrendHttpClient httpClient = mock(HotTrendHttpClient.class);
        when(httpClient.get(anyString(), anyMap(), eq(CrawlerRequestPolicy.backgroundNews())))
                .thenReturn("{\"ok\":1,\"data\":{\"realtime\":[]}}");
        WeiboFetcher fetcher = new WeiboFetcher(httpClient);

        assertTrue(fetcher.fetch().isEmpty());

        verify(httpClient).get(anyString(), anyMap(), eq(CrawlerRequestPolicy.backgroundNews()));
    }
}
