package com.stock.web.service;

import com.stock.crawler.model.Telegraph;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * 财经快讯服务 - 委托给 stock-crawler 库
 */
@Service
public class NewsService {

    private final com.stock.crawler.service.NewsService delegate =
            new com.stock.crawler.service.NewsService();

    public List<Telegraph> getTelegraphList() throws IOException {
        return delegate.getTelegraphList();
    }
}
