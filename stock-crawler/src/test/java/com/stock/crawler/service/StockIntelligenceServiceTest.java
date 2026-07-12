package com.stock.crawler.service;

import com.stock.crawler.model.AnnouncementItem;
import com.stock.crawler.model.FundFlowPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("个股分析素材服务测试")
class StockIntelligenceServiceTest {

    @Test
    @DisplayName("股票代码归一化应支持前缀和后缀格式")
    void normalizeCodeSupportsPrefixAndSuffixFormats() throws Exception {
        StockIntelligenceService service = new StockIntelligenceService();
        Method normalizeCode = StockIntelligenceService.class.getDeclaredMethod("normalizeCode", String.class);
        normalizeCode.setAccessible(true);

        assertEquals("600519", normalizeCode.invoke(service, "sh600519"));
        assertEquals("600519", normalizeCode.invoke(service, "600519.SH"));
        assertEquals("000001", normalizeCode.invoke(service, "000001.sz"));
    }

    @Test
    @DisplayName("新浪日级资金流应映射主力和各档净流入")
    void parseSinaDailyFundFlowMapsAmountBuckets() throws Exception {
        String json = """
                [{"opendate":"2026-07-10","r0_net":"6724104.00","r1_net":"-691794.76",\
                "r2_net":"-1141908.33","r3_net":"3164867.81"}]
                """;

        List<FundFlowPoint> result = new StockIntelligenceService().parseSinaDailyFundFlow(json);

        assertEquals(1, result.size());
        assertEquals("2026-07-10", result.getFirst().getTime());
        assertEquals(new BigDecimal("6032309.24"), result.getFirst().getMainNet());
        assertEquals(new BigDecimal("6724104.00"), result.getFirst().getSuperNet());
        assertEquals(new BigDecimal("-691794.76"), result.getFirst().getLargeNet());
        assertEquals(new BigDecimal("-1141908.33"), result.getFirst().getMidNet());
        assertEquals(new BigDecimal("3164867.81"), result.getFirst().getSmallNet());
    }

    @Test
    @DisplayName("巨潮公告应优先使用可直接访问的 PDF 链接")
    void parseAnnouncementsUsesStaticPdfUrl() throws Exception {
        String json = """
                {"announcements":[{"announcementId":"1225389844","announcementTitle":"担保公告",\
                "announcementTypeName":"临时公告","announcementTime":1782489600000,\
                "adjunctUrl":"finalpage/2026-06-27/1225389844.PDF"}]}
                """;

        List<AnnouncementItem> result = new StockIntelligenceService().parseAnnouncements(json);

        assertEquals(1, result.size());
        assertEquals("https://static.cninfo.com.cn/finalpage/2026-06-27/1225389844.PDF",
                result.getFirst().getUrl());
    }
}
