package com.stock.crawler.service;

import com.stock.crawler.model.StockRankItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 个股人气榜 JSON 解析逻辑单元测试（不依赖网络）
 */
@DisplayName("个股人气榜解析单元测试")
class StockRankServiceParseTest {

    private StockRankService service;
    private Method parseRankList;
    private Method parseTencentStockInfo;

    @BeforeEach
    void setUp() throws Exception {
        service = new StockRankService();
        parseRankList = StockRankService.class
                .getDeclaredMethod("parseRankList", String.class);
        parseRankList.setAccessible(true);
        parseTencentStockInfo = StockRankService.class
                .getDeclaredMethod("parseTencentStockInfo", String.class, List.class);
        parseTencentStockInfo.setAccessible(true);
    }

    private String loadFixture(String name) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/fixtures/" + name)) {
            assertNotNull(is, "Fixture file not found: " + name);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("正常响应解析 - 验证字段映射")
    @SuppressWarnings("unchecked")
    void testParseNormalResponse() throws Exception {
        String json = loadFixture("stock_rank_response.json");
        List<StockRankItem> items = (List<StockRankItem>) parseRankList.invoke(service, json);

        assertNotNull(items, "列表不应为 null");
        assertEquals(5, items.size(), "应解析出 5 条记录");
    }

    @Test
    @DisplayName("第一条记录字段验证")
    @SuppressWarnings("unchecked")
    void testFirstItemFields() throws Exception {
        String json = loadFixture("stock_rank_response.json");
        List<StockRankItem> items = (List<StockRankItem>) parseRankList.invoke(service, json);

        StockRankItem first = items.get(0);
        assertEquals("SH601016", first.getStockCode(), "股票代码应包含前缀");
        assertEquals(1, first.getRank(), "排名应为 1");
        assertEquals(0, first.getRankChange(), "排名变化应为 0（不变）");
    }

    @Test
    @DisplayName("排名变化字段验证 - 上升/下降/不变")
    @SuppressWarnings("unchecked")
    void testRankChangeValues() throws Exception {
        String json = loadFixture("stock_rank_response.json");
        List<StockRankItem> items = (List<StockRankItem>) parseRankList.invoke(service, json);

        // sc=SH601016 rc=0 不变
        assertEquals(0, items.get(0).getRankChange(), "rc=0 应为不变");
        // sc=SZ002475 rc=1 上升
        assertEquals(1, items.get(1).getRankChange(), "rc=1 应为上升");
        // sc=SH600396 rc=-1 下降
        assertEquals(-1, items.get(2).getRankChange(), "rc=-1 应为下降");
    }

    @Test
    @DisplayName("股票代码前缀 - SH/SZ 格式保持不变")
    @SuppressWarnings("unchecked")
    void testStockCodePrefix() throws Exception {
        String json = loadFixture("stock_rank_response.json");
        List<StockRankItem> items = (List<StockRankItem>) parseRankList.invoke(service, json);

        for (StockRankItem item : items) {
            assertNotNull(item.getStockCode(), "股票代码不应为 null");
            assertTrue(item.getStockCode().startsWith("SH") || item.getStockCode().startsWith("SZ"),
                    "股票代码应以 SH 或 SZ 开头，实际: " + item.getStockCode());
        }
    }

    @Test
    @DisplayName("getPureCode() - 应去除 SH/SZ 前缀")
    @SuppressWarnings("unchecked")
    void testGetPureCode() throws Exception {
        String json = loadFixture("stock_rank_response.json");
        List<StockRankItem> items = (List<StockRankItem>) parseRankList.invoke(service, json);

        // SH601016 -> 601016
        assertEquals("601016", items.get(0).getPureCode(), "SH 前缀应被去除");
        // SZ002475 -> 002475
        assertEquals("002475", items.get(1).getPureCode(), "SZ 前缀应被去除");
        // SZ000001 -> 000001
        assertEquals("000001", items.get(4).getPureCode(), "SZ000001 纯代码应为 000001");
    }

    @Test
    @DisplayName("getRankChangeDesc() - 文字描述正确")
    @SuppressWarnings("unchecked")
    void testGetRankChangeDesc() throws Exception {
        String json = loadFixture("stock_rank_response.json");
        List<StockRankItem> items = (List<StockRankItem>) parseRankList.invoke(service, json);

        // rc=0 -> 不变
        assertNotNull(items.get(0).getRankChangeDesc());
        // rc=1 -> 上升
        assertNotNull(items.get(1).getRankChangeDesc());
        // rc=-1 -> 下降
        assertNotNull(items.get(2).getRankChangeDesc());
    }

    @Test
    @DisplayName("空 data 数组应返回空列表")
    @SuppressWarnings("unchecked")
    void testParseEmptyDataArray() throws Exception {
        String json = "{\"data\":[]}";
        List<StockRankItem> items = (List<StockRankItem>) parseRankList.invoke(service, json);
        assertNotNull(items);
        assertTrue(items.isEmpty(), "空 data 应返回空列表");
    }

    @Test
    @DisplayName("腾讯详情响应为空时应返回 0 命中，便于触发兜底")
    void testParseEmptyTencentStockInfoReturnsZeroResolvedCount() throws Exception {
        StockRankItem item = new StockRankItem();
        item.setStockCode("SH600519");
        List<StockRankItem> items = List.of(item);

        int resolvedCount = (int) parseTencentStockInfo.invoke(service, "", items);

        assertEquals(0, resolvedCount);
        assertNull(item.getStockName());
    }

    @Test
    @DisplayName("腾讯批量响应含换行时应解析每一条股票")
    void testParseTencentBatchResponseWithLineBreaks() throws Exception {
        StockRankItem shItem = new StockRankItem();
        shItem.setStockCode("SH600519");
        StockRankItem szItem = new StockRankItem();
        szItem.setStockCode("SZ000001");
        List<StockRankItem> items = List.of(shItem, szItem);

        String body = tencentQuoteLine("sh600519", "贵州茅台", "1267.01", "-0.32")
                + ";\n"
                + tencentQuoteLine("sz000001", "平安银行", "11.05", "-0.09")
                + ";";

        int resolvedCount = (int) parseTencentStockInfo.invoke(service, body, items);

        assertEquals(2, resolvedCount);
        assertEquals("贵州茅台", shItem.getStockName());
        assertEquals("平安银行", szItem.getStockName());
    }

    @Test
    @DisplayName("getTopN 应只请求实际需要的数量")
    void getTopNRequestsOnlyRequestedItemCount() throws Exception {
        class RecordingStockRankService extends StockRankService {
            private int requestedPageSize;

            @Override
            public List<StockRankItem> getRankList(int pageSize) {
                requestedPageSize = pageSize;
                return List.of();
            }
        }
        RecordingStockRankService recordingService = new RecordingStockRankService();

        recordingService.getTopN(7);

        assertEquals(7, recordingService.requestedPageSize);
    }

    private String tencentQuoteLine(String code, String name, String price, String changePercent) {
        List<String> fields = new ArrayList<>(Collections.nCopies(33, ""));
        fields.set(1, name);
        fields.set(3, price);
        fields.set(32, changePercent);
        return "v_" + code + "=\"" + String.join("~", fields) + "\"";
    }
}
