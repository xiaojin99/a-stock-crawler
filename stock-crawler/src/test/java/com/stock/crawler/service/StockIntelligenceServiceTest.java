package com.stock.crawler.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

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
}
