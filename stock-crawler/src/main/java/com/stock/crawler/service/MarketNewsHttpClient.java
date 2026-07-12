package com.stock.crawler.service;

import com.stock.crawler.util.CrawlerRequestPolicy;
import java.io.IOException;
import java.util.Map;

@FunctionalInterface
interface MarketNewsHttpClient {

    String get(String url, Map<String, String> headers, CrawlerRequestPolicy policy)
            throws IOException;
}
