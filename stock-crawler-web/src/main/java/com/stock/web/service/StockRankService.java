package com.stock.web.service;

import com.stock.crawler.model.StockRankItem;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * 个股人气榜服务 - 委托给 stock-crawler 库
 */
@Service
public class StockRankService {

    private final com.stock.crawler.service.StockRankService delegate =
            new com.stock.crawler.service.StockRankService();

    public List<StockRankItem> getRankList(int pageSize) throws IOException {
        return delegate.getRankList(pageSize);
    }

    public List<StockRankItem> getRankList() throws IOException {
        return delegate.getRankList();
    }
}
