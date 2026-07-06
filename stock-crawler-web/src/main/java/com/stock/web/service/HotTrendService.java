package com.stock.web.service;

import com.stock.crawler.model.HotItem;
import com.stock.crawler.model.HotTrendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * 舆情热点服务 - 委托给 stock-crawler 库
 */
@Service
public class HotTrendService {

    private static final Logger log = LoggerFactory.getLogger(HotTrendService.class);

    private final com.stock.crawler.service.HotTrendService delegate =
            new com.stock.crawler.service.HotTrendService();

    public List<HotItem> getWeiboHot() throws IOException {
        return getWeiboHot(50);
    }

    public List<HotItem> getWeiboHot(int limit) throws IOException {
        HotTrendResult result = delegate.getHotTrend("weibo");
        if (result.hasError()) {
            log.warn("Weibo hot trend failed: {}", result.getError());
            throw new IOException("Weibo hot trend error: " + result.getError());
        }
        List<HotItem> items = result.getItems();
        if (items == null) return Collections.emptyList();
        return items.size() > limit ? items.subList(0, limit) : items;
    }

    public List<HotItem> getBaiduHot() throws IOException {
        return getBaiduHot(50);
    }

    public List<HotItem> getBaiduHot(int limit) throws IOException {
        HotTrendResult result = delegate.getHotTrend("baidu");
        if (result.hasError()) {
            log.warn("Baidu hot trend failed: {}", result.getError());
            throw new IOException("Baidu hot trend error: " + result.getError());
        }
        List<HotItem> items = result.getItems();
        if (items == null) return Collections.emptyList();
        return items.size() > limit ? items.subList(0, limit) : items;
    }
}
