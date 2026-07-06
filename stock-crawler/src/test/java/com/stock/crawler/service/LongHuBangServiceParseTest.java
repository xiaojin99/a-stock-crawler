package com.stock.crawler.service;

import com.stock.crawler.model.LongHuBangItem;
import com.stock.crawler.service.LongHuBangService.LongHuBangResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 龙虎榜 JSON 解析逻辑单元测试（不依赖网络）
 */
@DisplayName("龙虎榜解析单元测试")
class LongHuBangServiceParseTest {

    private LongHuBangService service;
    private Method parseLongHuBangResponse;

    @BeforeEach
    void setUp() throws Exception {
        service = new LongHuBangService();
        parseLongHuBangResponse = LongHuBangService.class
                .getDeclaredMethod("parseLongHuBangResponse", String.class);
        parseLongHuBangResponse.setAccessible(true);
    }

    private String loadFixture(String name) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/fixtures/" + name)) {
            assertNotNull(is, "Fixture file not found: " + name);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("正常响应解析 - 验证字段映射")
    void testParseNormalResponse() throws Exception {
        String json = loadFixture("longhubang_response.json");
        LongHuBangResult result = (LongHuBangResult) parseLongHuBangResponse.invoke(service, json);

        assertNotNull(result, "解析结果不应为 null");
        assertNotNull(result.getItems(), "items 不应为 null");
        assertEquals(2, result.getItems().size(), "应解析出 2 条记录");
        assertEquals(150, result.getTotal(), "total 应为 150");

        LongHuBangItem first = result.getItems().get(0);
        assertEquals("000001", first.getCode(), "股票代码应为 000001");
        assertEquals("000001.SZ", first.getSecuCode(), "SecuCode 应正确");
        assertEquals("平安银行", first.getName(), "股票名称应为 平安银行");
        assertEquals("2026-03-25", first.getTradeDate(), "交易日期应截取为 YYYY-MM-DD 格式");
        assertEquals(11.5, first.getClosePrice(), 0.001, "收盘价应为 11.5");
        assertEquals(9.98, first.getChangePercent(), 0.001, "涨跌幅应为 9.98");
        assertEquals(12345678.0, first.getNetBuyAmt(), 0.001, "净买入金额应正确");
        assertEquals("连续三日涨幅偏离值偏高", first.getReason(), "上榜原因应正确");
    }

    @Test
    @DisplayName("日期截断格式验证 - YYYY-MM-DD HH:mm:ss -> YYYY-MM-DD")
    void testTradeDateTruncation() throws Exception {
        String json = loadFixture("longhubang_response.json");
        LongHuBangResult result = (LongHuBangResult) parseLongHuBangResponse.invoke(service, json);

        for (LongHuBangItem item : result.getItems()) {
            assertNotNull(item.getTradeDate(), "交易日期不应为 null");
            assertTrue(item.getTradeDate().matches("\\d{4}-\\d{2}-\\d{2}"),
                    "交易日期格式应为 YYYY-MM-DD，实际: " + item.getTradeDate());
        }
    }

    @Test
    @DisplayName("解析失败响应 - success=false 应抛出 IOException")
    void testParseFailureResponse() {
        String json = "{\"success\":false,\"message\":\"接口异常\",\"result\":null}";
        Exception ex = assertThrows(Exception.class, () ->
                parseLongHuBangResponse.invoke(service, json));
        assertTrue(ex.getCause() instanceof IOException, "应抛出 IOException");
        assertTrue(ex.getCause().getMessage().contains("接口异常"),
                "异常信息应包含 API 返回的 message");
    }

    @Test
    @DisplayName("data 数组为空时应返回空列表")
    void testParseEmptyDataArray() throws Exception {
        String json = "{\"success\":true,\"message\":\"OK\",\"result\":{\"count\":0,\"data\":[]}}";
        LongHuBangResult result = (LongHuBangResult) parseLongHuBangResponse.invoke(service, json);
        assertNotNull(result.getItems(), "items 不应为 null");
        assertTrue(result.getItems().isEmpty(), "items 应为空列表");
        assertEquals(0, result.getTotal(), "total 应为 0");
    }

    @Test
    @DisplayName("第二条记录字段验证 - 贵州茅台")
    void testParseSecondItem() throws Exception {
        String json = loadFixture("longhubang_response.json");
        LongHuBangResult result = (LongHuBangResult) parseLongHuBangResponse.invoke(service, json);

        LongHuBangItem second = result.getItems().get(1);
        assertEquals("600519", second.getCode());
        assertEquals("贵州茅台", second.getName());
        assertEquals(1680.0, second.getClosePrice(), 0.001);
        assertEquals(7.02, second.getChangePercent(), 0.001);
        assertEquals("2026-03-25", second.getTradeDate());
    }
}
