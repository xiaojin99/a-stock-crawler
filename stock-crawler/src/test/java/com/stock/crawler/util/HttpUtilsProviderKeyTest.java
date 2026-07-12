package com.stock.crawler.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HttpUtilsProviderKeyTest {

    @Test
    void providerKey_groupsAllEastMoneyAndDfcfwHosts() {
        assertEquals(
                "eastmoney",
                HttpUtils.providerKeyForTest("https://push2.eastmoney.com/api/quote"));
        assertEquals(
                "eastmoney",
                HttpUtils.providerKeyForTest("https://push2his.eastmoney.com/api/kline"));
        assertEquals(
                "eastmoney",
                HttpUtils.providerKeyForTest("https://finance.dfcfw.com/path"));
    }
}
