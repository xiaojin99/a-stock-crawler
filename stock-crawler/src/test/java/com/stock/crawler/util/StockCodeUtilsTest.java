package com.stock.crawler.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StockCodeUtilsTest {

    @Test
    void normalizesSupportedAStockFormatsWithMarketPrefix() {
        assertEquals("sh600519", StockCodeUtils.normalizeWithMarket("600519"));
        assertEquals("sh600519", StockCodeUtils.normalizeWithMarket("SH600519"));
        assertEquals("sh600519", StockCodeUtils.normalizeWithMarket("600519.SH"));
        assertEquals("sz000001", StockCodeUtils.normalizeWithMarket("000001.sz"));
        assertEquals("bj430047", StockCodeUtils.normalizeWithMarket("430047"));
    }

    @Test
    void stripsMarketPrefixForProviderFilters() {
        assertEquals("600519", StockCodeUtils.stripMarket("SH600519"));
        assertEquals("600519", StockCodeUtils.stripMarket("600519.SH"));
        assertEquals("000001", StockCodeUtils.stripMarket("000001"));
    }

    @Test
    void rejectsInvalidStockCodes() {
        assertThrows(IllegalArgumentException.class,
                () -> StockCodeUtils.normalizeWithMarket("javascript:alert(1)"));
        assertThrows(IllegalArgumentException.class,
                () -> StockCodeUtils.stripMarket("600519&extra=true"));
    }

    @Test
    void clampsNumericArguments() {
        assertEquals(1, StockCodeUtils.clamp(0, 1, 120));
        assertEquals(120, StockCodeUtils.clamp(500, 1, 120));
        assertEquals(30, StockCodeUtils.clamp(30, 1, 120));
    }
}
