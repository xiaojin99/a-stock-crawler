package com.stock.crawler.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.crawler.model.ResearchReport;
import com.stock.crawler.util.HttpUtils;
import com.stock.crawler.util.StockCodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 研报服务
 * 调用东方财富研报 API 获取个股研报数据
 */
public class ResearchReportService {

    private static final Logger log = LoggerFactory.getLogger(ResearchReportService.class);
    private static final String EASTMONEY_REPORT_API = "https://reportapi.eastmoney.com/report/list";

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 研报响应结果
     */
    public static class ReportResponse {
        private List<ResearchReport> data;
        private int totalPage;
        private int totalCount;

        public List<ResearchReport> getData() {
            return data;
        }

        public void setData(List<ResearchReport> data) {
            this.data = data;
        }

        public int getTotalPage() {
            return totalPage;
        }

        public void setTotalPage(int totalPage) {
            this.totalPage = totalPage;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(int totalCount) {
            this.totalCount = totalCount;
        }
    }

    /**
     * 获取个股研报列表
     *
     * @param stockCode 股票代码 (如 "000001"，支持带前缀如 "sz000001")
     * @param pageSize  每页数量
     * @param pageNo    页码
     * @return 研报响应
     */
    public ReportResponse getResearchReports(String stockCode, int pageSize, int pageNo) throws IOException {
        String code = StockCodeUtils.stripMarket(stockCode);
        int safePageSize = StockCodeUtils.clamp(pageSize, 1, 100);
        int safePageNo = Math.max(pageNo, 1);

        int nextYear = Year.now().getValue() + 1;
        String url = String.format("%s?industryCode=*&pageSize=%d&industry=*&rating=*&ratingChange=*" +
                        "&beginTime=2020-01-01&endTime=%d-01-01&pageNo=%d&fields=&qType=0&orgCode=&code=%s&rcode=",
                EASTMONEY_REPORT_API, safePageSize, nextYear, safePageNo, code);

        log.info("Fetching research reports for stock: {}", code);

        Map<String, String> headers = Map.of(
                "Referer", "https://data.eastmoney.com/"
        );

        String json = HttpUtils.getEastMoney(url, headers);
        ReportResponse response = parseReportResponse(json, code);
        log.info("Fetched {} research reports for stock: {}, total: {}",
                response.getData().size(), code, response.getTotalCount());
        return response;
    }

    /**
     * 获取个股研报（默认 10 条）
     */
    public List<ResearchReport> getResearchReports(String stockCode) throws IOException {
        ReportResponse response = getResearchReports(stockCode, 10, 1);
        List<ResearchReport> data = response.getData();
        if (data == null || data.isEmpty()) {
            log.warn("No research reports found for stock: {}", stockCode);
        }
        return data != null ? data : new java.util.ArrayList<>();
    }

    /**
     * 根据研报 infoCode 生成 PDF 下载链接
     */
    public String getReportPdfUrl(String infoCode) {
        if (infoCode == null || infoCode.isEmpty()) {
            return "";
        }
        return String.format("https://pdf.dfcfw.com/pdf/H3_%s_1.pdf", infoCode);
    }

    /**
     * 解析研报 API 响应
     */
    private ReportResponse parseReportResponse(String json, String stockCode) throws IOException {
        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IOException("Failed to parse research report JSON for stock " + stockCode + ": " + e.getMessage(), e);
        }

        ReportResponse response = new ReportResponse();
        List<ResearchReport> reports = new ArrayList<>();

        JsonNode dataNode = root.path("data");
        if (dataNode.isMissingNode()) {
            log.warn("Missing 'data' field in research report response for stock: {}", stockCode);
        } else if (dataNode.isArray()) {
            for (JsonNode node : dataNode) {
                ResearchReport report = new ResearchReport();
                report.setTitle(textValue(node, "title"));
                report.setStockName(textValue(node, "stockName"));
                report.setStockCode(textValue(node, "stockCode"));
                report.setOrgSName(textValue(node, "orgSName"));
                report.setPublishDate(textValue(node, "publishDate"));
                report.setPredictThisYearEps(textValue(node, "predictThisYearEps"));
                report.setPredictThisYearPe(textValue(node, "predictThisYearPe"));
                report.setEmRatingName(textValue(node, "emRatingName"));
                report.setResearcher(textValue(node, "researcher"));
                report.setInfoCode(textValue(node, "infoCode"));
                report.setIndustry(textValue(node, "indvInduName"));
                report.setEncodeUrl(textValue(node, "encodeUrl"));

                reports.add(report);
            }
        }

        response.setData(reports);
        response.setTotalPage(root.path("TotalPage").asInt(0));
        response.setTotalCount(root.path("TotalCount").asInt(0));

        log.info("Parsed {} research reports, total: {}", reports.size(), response.getTotalCount());
        return response;
    }

    private String textValue(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    /**
     * 将研报格式化为文本
     */
    public String formatReportsToText(List<ResearchReport> reports) {
        if (reports == null || reports.isEmpty()) {
            return "暂无研报数据";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < reports.size(); i++) {
            ResearchReport r = reports.get(i);
            sb.append(String.format("%d. 【%s】%s\n", i + 1,
                    r.getEmRatingName() != null ? r.getEmRatingName() : "",
                    r.getTitle() != null ? r.getTitle() : ""));
            sb.append(String.format("   券商: %s | 研究员: %s\n",
                    r.getOrgSName() != null ? r.getOrgSName() : "",
                    r.getResearcher() != null ? r.getResearcher() : ""));
            sb.append(String.format("   发布日期: %s | 行业: %s\n",
                    r.getPublishDate() != null ? r.getPublishDate() : "",
                    r.getIndustry() != null ? r.getIndustry() : ""));
            if (r.getPredictThisYearEps() != null || r.getPredictThisYearPe() != null) {
                sb.append(String.format("   预测EPS: %s | 预测PE: %s\n",
                        r.getPredictThisYearEps() != null ? r.getPredictThisYearEps() : "",
                        r.getPredictThisYearPe() != null ? r.getPredictThisYearPe() : ""));
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
