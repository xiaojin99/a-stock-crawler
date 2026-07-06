package com.stock.web.service;

import com.stock.crawler.model.MarketHotBoard;
import com.stock.crawler.model.MarketNewsItem;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 市场快讯 Web 服务 - 委托给 stock-crawler 的 MarketNewsService
 * 添加 Spring 生命周期管理（@PostConstruct 初始化 + @Scheduled 定时刷新缓存）
 */
@Service
public class MarketNewsWebService {

    private static final Logger log = LoggerFactory.getLogger(MarketNewsWebService.class);

    private final com.stock.crawler.service.MarketNewsService delegate =
            new com.stock.crawler.service.MarketNewsService();

    @PostConstruct
    public void initCache() {
        try {
            delegate.refreshHotNewsCache();
        } catch (Exception e) {
            log.warn("market_news_init_cache_failed", e);
        }
    }

    @Scheduled(fixedDelay = 300_000L, initialDelay = 300_000L)
    public void refreshHotNewsCache() {
        try {
            delegate.refreshHotNewsCache();
        } catch (Exception e) {
            log.warn("market_news_refresh_cache_failed", e);
        }
    }

    public List<MarketNewsItem> listHotNews(String keyword, Integer limit) {
        return delegate.listHotNews(keyword, limit);
    }

    public List<MarketHotBoard> listHotBoards(Integer limit) {
        return delegate.listHotBoards(limit);
    }
}
