package com.stock.crawler.fetcher;

import com.stock.crawler.util.CrawlerRequestPolicy;
import java.io.IOException;
import java.util.Map;

@FunctionalInterface
interface HotTrendHttpClient {

    String get(String url, Map<String, String> headers, CrawlerRequestPolicy policy)
            throws IOException;
}
