package com.stock.crawler.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stock.crawler.util.CrawlerRequestPolicy;
import org.junit.jupiter.api.Test;

class LongHuBangRequestPolicyTest {

    @Test
    void listRefresh_usesBackgroundNewsPolicy() throws Exception {
        LongHuBangHttpClient httpClient = mock(LongHuBangHttpClient.class);
        when(httpClient.getEastMoney(
                anyString(), anyMap(), eq(CrawlerRequestPolicy.backgroundNews())))
                .thenReturn("{\"success\":true,\"result\":{\"data\":[],\"count\":0}}");
        LongHuBangService service = new LongHuBangService(httpClient);

        assertTrue(service.getLongHuBangList(4, 1, null).getItems().isEmpty());

        verify(httpClient).getEastMoney(
                anyString(), anyMap(), eq(CrawlerRequestPolicy.backgroundNews()));
    }
}
