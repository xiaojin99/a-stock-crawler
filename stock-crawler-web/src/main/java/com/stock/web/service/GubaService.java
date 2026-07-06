package com.stock.web.service;

import com.stock.crawler.model.GubaComment;
import com.stock.crawler.model.GubaPost;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * 股吧服务 - 委托给 stock-crawler 库
 */
@Service
public class GubaService {

    private final com.stock.crawler.service.GubaService delegate =
            new com.stock.crawler.service.GubaService();

    public List<GubaPost> getPostList(String stockCode, int pageSize) throws IOException {
        return delegate.getPostList(stockCode, pageSize, 1);
    }

    public List<GubaComment> getTop3PostComments(String stockCode) throws IOException {
        return delegate.getTop3PostComments(stockCode);
    }
}
