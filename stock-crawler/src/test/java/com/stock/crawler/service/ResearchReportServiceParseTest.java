package com.stock.crawler.service;

import com.stock.crawler.model.ResearchReport;
import com.stock.crawler.service.ResearchReportService.ReportResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 研报 JSON 解析逻辑单元测试（不依赖网络）
 */
@DisplayName("研报解析单元测试")
class ResearchReportServiceParseTest {

    private ResearchReportService service;
    private Method parseReportResponse;

    @BeforeEach
    void setUp() throws Exception {
        service = new ResearchReportService();
        parseReportResponse = ResearchReportService.class
                .getDeclaredMethod("parseReportResponse", String.class, String.class);
        parseReportResponse.setAccessible(true);
    }

    private String loadFixture(String name) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/fixtures/" + name)) {
            assertNotNull(is, "Fixture file not found: " + name);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("正常响应解析 - 验证研报字段映射")
    void testParseNormalResponse() throws Exception {
        String json = loadFixture("research_report_response.json");
        ReportResponse response = (ReportResponse) parseReportResponse.invoke(service, json, "000001");

        assertNotNull(response, "响应不应为 null");
        assertNotNull(response.getData(), "data 不应为 null");
        assertEquals(2, response.getData().size(), "应解析出 2 条研报");
        assertEquals(85, response.getTotalCount(), "totalCount 应为 85");
        assertEquals(9, response.getTotalPage(), "totalPage 应为 9");
    }

    @Test
    @DisplayName("第一条研报字段验证")
    void testFirstReportFields() throws Exception {
        String json = loadFixture("research_report_response.json");
        ReportResponse response = (ReportResponse) parseReportResponse.invoke(service, json, "000001");

        ResearchReport first = response.getData().get(0);
        assertEquals("平安银行2026年年报业绩点评：归母净利润增长8.9%，不良率下降", first.getTitle(),
                "标题应正确");
        assertEquals("000001", first.getStockCode(), "股票代码应为 000001");
        assertEquals("平安银行", first.getStockName(), "股票名称应正确");
        assertEquals("中信证券", first.getOrgSName(), "券商简称应正确");
        assertEquals("肖斐斐", first.getResearcher(), "研究员应正确");
        assertEquals("买入", first.getEmRatingName(), "评级应为 买入");
        assertEquals("2026-03-20", first.getPublishDate(), "发布日期应正确");
        assertEquals("AP202603200001234567", first.getInfoCode(), "infoCode 应正确");
        assertEquals("银行", first.getIndustry(), "行业应正确");
        assertEquals("1.58", first.getPredictThisYearEps(), "预测EPS应正确");
        assertEquals("https://pdf.dfcfw.com/pdf/H3_AP202603200001234567_1.pdf",
                first.getEncodeUrl(), "研报链接应使用可直接访问的 PDF 地址");
    }

    @Test
    @DisplayName("第二条研报评级验证")
    void testSecondReportRating() throws Exception {
        String json = loadFixture("research_report_response.json");
        ReportResponse response = (ReportResponse) parseReportResponse.invoke(service, json, "000001");

        ResearchReport second = response.getData().get(1);
        assertEquals("增持", second.getEmRatingName(), "第二条评级应为 增持");
        assertEquals("海通证券", second.getOrgSName(), "第二条券商应为 海通证券");
    }

    @Test
    @DisplayName("空 data 数组应返回空列表")
    void testParseEmptyDataArray() throws Exception {
        String json = "{\"TotalCount\":0,\"TotalPage\":0,\"data\":[]}";
        ReportResponse response = (ReportResponse) parseReportResponse.invoke(service, json, "999999");

        assertNotNull(response.getData(), "data 不应为 null");
        assertTrue(response.getData().isEmpty(), "data 应为空列表");
        assertEquals(0, response.getTotalCount());
    }

    @Test
    @DisplayName("PDF 链接生成 - 验证 URL 格式")
    void testGetReportPdfUrl() {
        String infoCode = "AP202603200001234567";
        String pdfUrl = service.getReportPdfUrl(infoCode);

        assertNotNull(pdfUrl, "PDF URL 不应为 null");
        assertEquals("https://pdf.dfcfw.com/pdf/H3_AP202603200001234567_1.pdf", pdfUrl,
                "PDF URL 格式应正确");
    }

    @Test
    @DisplayName("PDF 链接 - 空 infoCode 返回空字符串")
    void testGetReportPdfUrlWithEmpty() {
        assertEquals("", service.getReportPdfUrl(null));
        assertEquals("", service.getReportPdfUrl(""));
    }

    @Test
    @DisplayName("格式化输出 - 空列表返回提示文字")
    void testFormatEmptyReports() {
        String result = service.formatReportsToText(null);
        assertEquals("暂无研报数据", result);

        result = service.formatReportsToText(List.of());
        assertEquals("暂无研报数据", result);
    }
}
