package com.stock.crawler.datasource;

import com.stock.crawler.model.KLineData;
import com.stock.crawler.model.StockQuote;

import java.util.List;

/**
 * 行情数据源接口
 */
public interface MarketDataSource {

    /**
     * 获取数据源名称
     */
    String getName();

    /**
     * 批量获取实时行情
     *
     * @param stockCodes 股票代码列表（带前缀，如 sh600519, sz000001）
     * @return 行情列表
     */
    List<StockQuote> getRealTimeQuotes(List<String> stockCodes);

    /**
     * 获取K线数据
     *
     * @param stockCode 股票代码
     * @param period    周期（day/week/month）
     * @param days      天数
     * @return K线数据列表
     */
    List<KLineData> getKLineData(String stockCode, String period, int days);

    /**
     * 搜索股票
     *
     * @param keyword 关键词
     * @return 股票列表
     */
    List<StockQuote> searchStock(String keyword);

    /**
     * 检查数据源是否可用
     */
    boolean isAvailable();

    /**
     * 获取优先级（数字越小优先级越高）
     */
    default int getPriority() {
        return 100;
    }
}
