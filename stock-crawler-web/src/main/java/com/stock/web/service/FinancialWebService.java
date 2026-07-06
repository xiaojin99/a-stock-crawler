package com.stock.web.service;

import com.stock.crawler.model.FinancialIndicator;
import com.stock.crawler.model.ShareholderConcentration;
import com.stock.crawler.service.FinancialService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * 财务数据服务 - 委托给 stock-crawler 库
 */
@Service
public class FinancialWebService {

    private final FinancialService delegate = new FinancialService();

    public List<FinancialIndicator> getFinancialIndicators(String stockCode, int count) throws IOException {
        return delegate.getMainFinancialIndicators(stockCode, count);
    }

    public List<ShareholderConcentration> getShareholderConcentration(String stockCode, int count) throws IOException {
        return delegate.getShareholderConcentration(stockCode, count);
    }

    public String formatFinancialIndicators(List<FinancialIndicator> indicators) {
        return delegate.formatFinancialIndicators(indicators);
    }

    public String formatShareholderConcentration(List<ShareholderConcentration> data) {
        return delegate.formatShareholderConcentration(data);
    }
}
