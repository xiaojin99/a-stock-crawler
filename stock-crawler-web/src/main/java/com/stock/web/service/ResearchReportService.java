package com.stock.web.service;

import com.stock.crawler.model.ResearchReport;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * 研报服务 - 委托给 stock-crawler 库
 */
@Service
public class ResearchReportService {

    private final com.stock.crawler.service.ResearchReportService delegate =
            new com.stock.crawler.service.ResearchReportService();

    public List<ResearchReport> getResearchReports(String stockCode) throws IOException {
        return delegate.getResearchReports(stockCode);
    }

    public List<ResearchReport> getResearchReports(String stockCode, int pageSize, int pageNo) throws IOException {
        List<ResearchReport> data = delegate.getResearchReports(stockCode, pageSize, pageNo).getData();
        return data != null ? data : List.of();
    }

    public String getReportPdfUrl(String infoCode) {
        return delegate.getReportPdfUrl(infoCode);
    }
}
