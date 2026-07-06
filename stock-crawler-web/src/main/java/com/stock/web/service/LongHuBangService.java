package com.stock.web.service;

import com.stock.crawler.model.LongHuBangItem;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * 龙虎榜服务 - 委托给 stock-crawler 库
 */
@Service
public class LongHuBangService {

    private final com.stock.crawler.service.LongHuBangService delegate =
            new com.stock.crawler.service.LongHuBangService();

    public List<LongHuBangItem> getLongHuBangList() throws IOException {
        return delegate.getLongHuBangList().getItems();
    }

    public List<LongHuBangItem> getLongHuBangList(int pageSize, int pageNo, String tradeDate) throws IOException {
        return delegate.getLongHuBangList(pageSize, pageNo, tradeDate).getItems();
    }
}
