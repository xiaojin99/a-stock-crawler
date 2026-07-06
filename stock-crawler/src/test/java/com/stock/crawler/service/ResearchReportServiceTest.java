package com.stock.crawler.service;

import com.stock.crawler.model.ResearchReport;
import com.stock.crawler.service.ResearchReportService.ReportResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 研报服务集成测试（需要网络，运行: mvn test -Dintegration=true）
 */
@Tag("integration")
@DisplayName("研报服务集成测试")
class ResearchReportServiceTest {

    private ResearchReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ResearchReportService();
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("测试获取个股研报 - 平安銀行(000001)")
    void testGetResearchReports() throws IOException {
        // 平安银行
        String stockCode = "000001";
        List<ResearchReport> reports = reportService.getResearchReports(stockCode);

        // 验证返回数据非空
        assertNotNull(reports, "研报列表不应为 null");
        assertFalse(reports.isEmpty(), "研报列表不应为空");

        // 验证第一条数据字段
        ResearchReport first = reports.get(0);
        assertNotNull(first.getTitle(), "研报标题不应为 null");
        assertFalse(first.getTitle().isEmpty(), "研报标题不应为空");

        System.out.println("=== 平安银行研报列表 ===");
        System.out.println("共获取 " + reports.size() + " 条研报");
        for (int i = 0; i < Math.min(5, reports.size()); i++) {
            ResearchReport r = reports.get(i);
            System.out.printf("%d. [%s] %s%n", i + 1, r.getEmRatingName(), r.getTitle());
            System.out.printf("   券商: %s | 研究员: %s%n", r.getOrgSName(), r.getResearcher());
            System.out.printf("   发布日期: %s%n", r.getPublishDate());
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("测试获取研报（分页）")
    void testGetResearchReportsWithPaging() throws IOException {
        ReportResponse response = reportService.getResearchReports("000001", 5, 1);

        assertNotNull(response, "响应不应为 null");
        assertNotNull(response.getData(), "数据列表不应为 null");
        assertTrue(response.getData().size() <= 5, "返回数量应不超过 pageSize");
        // API 可能返回 totalCount=0，所以只验证数据不为空
        assertFalse(response.getData().isEmpty(), "数据列表不应为空");

        System.out.println("=== 分页研报测试 ===");
        System.out.println("每页数量: 5");
        System.out.println("当前页: 1");
        System.out.println("返回数量: " + response.getData().size());
        System.out.println("总数量: " + response.getTotalCount());
        System.out.println("总页数: " + response.getTotalPage());
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("测试研报格式化输出")
    void testFormatReportsToText() throws IOException {
        List<ResearchReport> reports = reportService.getResearchReports("600519"); // 贵州茅台
        String text = reportService.formatReportsToText(reports);

        assertNotNull(text, "格式化文本不应为 null");
        assertFalse(text.isEmpty(), "格式化文本不应为空");

        System.out.println("=== 研报格式化输出 ===");
        System.out.println(text);
    }

    @Test
    @DisplayName("测试生成研报 PDF 链接")
    void testGetReportPdfUrl() {
        String infoCode = "AP20240101123456789";
        String pdfUrl = reportService.getReportPdfUrl(infoCode);

        assertNotNull(pdfUrl, "PDF URL 不应为 null");
        assertTrue(pdfUrl.contains(infoCode), "PDF URL 应包含 infoCode");
        assertTrue(pdfUrl.startsWith("https://"), "PDF URL 应以 https:// 开头");

        System.out.println("=== PDF 链接测试 ===");
        System.out.println("InfoCode: " + infoCode);
        System.out.println("PDF URL: " + pdfUrl);
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    @DisplayName("测试不同股票代码格式")
    void testDifferentStockCodeFormats() throws IOException {
        // 测试带前缀的股票代码
        List<ResearchReport> reports1 = reportService.getResearchReports("sz000001");
        List<ResearchReport> reports2 = reportService.getResearchReports("sh600519");

        System.out.println("=== 不同股票代码格式测试 ===");
        System.out.println("sz000001 研报数量: " + reports1.size());
        System.out.println("sh600519 研报数量: " + reports2.size());
    }
}
