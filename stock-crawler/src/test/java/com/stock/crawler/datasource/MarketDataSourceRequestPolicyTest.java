package com.stock.crawler.datasource;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stock.crawler.util.CrawlerRequestPolicy;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MarketDataSourceRequestPolicyTest {

    @Test
    void tencentQuote_usesInteractiveSingleAttemptPolicy() throws Exception {
        TencentDataSource.QuoteBodyFetcher fetcher = mock(TencentDataSource.QuoteBodyFetcher.class);
        when(fetcher.fetch(anyString(), eq(CrawlerRequestPolicy.interactive()))).thenReturn("");
        TencentDataSource source = new TencentDataSource(fetcher);

        assertTrue(source.getRealTimeQuotes(List.of("sh600519")).isEmpty());

        verify(fetcher).fetch(anyString(), eq(CrawlerRequestPolicy.interactive()));
    }

    @Test
    void sinaQuote_usesInteractiveSingleAttemptPolicy() throws Exception {
        SinaDataSource.HttpBodyFetcher fetcher = mock(SinaDataSource.HttpBodyFetcher.class);
        when(fetcher.get(anyString(), eq(Map.of("Referer", "http://finance.sina.com.cn")),
                eq(CrawlerRequestPolicy.interactive()))).thenReturn("");
        SinaDataSource source = new SinaDataSource(fetcher);

        assertTrue(source.getRealTimeQuotes(List.of("sh600519")).isEmpty());

        verify(fetcher).get(anyString(), eq(Map.of("Referer", "http://finance.sina.com.cn")),
                eq(CrawlerRequestPolicy.interactive()));
    }

    @Test
    void eastMoneyKline_usesInteractiveSingleAttemptPolicy() throws Exception {
        EastMoneyDataSource.HttpBodyFetcher fetcher =
                mock(EastMoneyDataSource.HttpBodyFetcher.class);
        when(fetcher.getEastMoney(anyString(),
                eq(Map.of("Referer", "https://quote.eastmoney.com/")),
                eq(CrawlerRequestPolicy.interactive())))
                .thenReturn("{\"data\":{\"klines\":[]}}");
        EastMoneyDataSource source = new EastMoneyDataSource(fetcher);

        assertTrue(source.getKLineData("sh600519", "day", 5).isEmpty());

        verify(fetcher).getEastMoney(anyString(),
                eq(Map.of("Referer", "https://quote.eastmoney.com/")),
                eq(CrawlerRequestPolicy.interactive()));
    }
}
