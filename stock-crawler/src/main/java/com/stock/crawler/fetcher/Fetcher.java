package com.stock.crawler.fetcher;

import com.stock.crawler.model.HotItem;

import java.io.IOException;
import java.util.List;

/**
 * 热点数据获取器接口
 */
public interface Fetcher {

    /**
     * 获取平台标识
     */
    String platform();

    /**
     * 获取平台中文名
     */
    String platformCN();

    /**
     * 获取热点数据
     */
    List<HotItem> fetch() throws IOException;
}
