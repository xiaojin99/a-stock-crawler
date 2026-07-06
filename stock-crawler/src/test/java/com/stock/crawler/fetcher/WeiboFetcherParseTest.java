package com.stock.crawler.fetcher;

import com.stock.crawler.model.HotItem;
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
 * 微博热搜 JSON 解析逻辑单元测试（不依赖网络）
 */
@DisplayName("微博热搜解析单元测试")
class WeiboFetcherParseTest {

    private WeiboFetcher fetcher;
    private Method parseWeiboResponse;

    @BeforeEach
    void setUp() throws Exception {
        fetcher = new WeiboFetcher();
        parseWeiboResponse = WeiboFetcher.class
                .getDeclaredMethod("parseWeiboResponse", String.class);
        parseWeiboResponse.setAccessible(true);
    }

    private String loadFixture(String name) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/fixtures/" + name)) {
            assertNotNull(is, "Fixture file not found: " + name);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("正常响应解析 - 验证热搜条目数量和字段")
    @SuppressWarnings("unchecked")
    void testParseNormalResponse() throws Exception {
        String json = loadFixture("weibo_hottrend_response.json");
        List<HotItem> items = (List<HotItem>) parseWeiboResponse.invoke(fetcher, json);

        assertNotNull(items, "列表不应为 null");
        assertFalse(items.isEmpty(), "列表不应为空");
        assertEquals(10, items.size(), "应解析出 10 条热搜");
    }

    @Test
    @DisplayName("第一条热搜字段验证")
    @SuppressWarnings("unchecked")
    void testFirstItemFields() throws Exception {
        String json = loadFixture("weibo_hottrend_response.json");
        List<HotItem> items = (List<HotItem>) parseWeiboResponse.invoke(fetcher, json);

        HotItem first = items.get(0);
        assertEquals("人工智能新突破", first.getTitle(), "标题应正确");
        assertEquals(1, first.getRank(), "第一条排名应为 1");
        assertEquals("weibo", first.getPlatform(), "平台应为 weibo");
        assertEquals(9876543, first.getHotScore().intValue(), "热度值应正确");
        assertNotNull(first.getUrl(), "URL 不应为 null");
        assertTrue(first.getUrl().contains("s.weibo.com"), "URL 应指向微博搜索");
    }

    @Test
    @DisplayName("排名连续性验证 - 从 1 开始连续递增")
    @SuppressWarnings("unchecked")
    void testRankSequential() throws Exception {
        String json = loadFixture("weibo_hottrend_response.json");
        List<HotItem> items = (List<HotItem>) parseWeiboResponse.invoke(fetcher, json);

        for (int i = 0; i < items.size(); i++) {
            assertEquals(i + 1, items.get(i).getRank(),
                    "第 " + (i + 1) + " 条排名应为 " + (i + 1));
        }
    }

    @Test
    @DisplayName("ID 格式验证 - 应以 weibo_ 开头")
    @SuppressWarnings("unchecked")
    void testIdFormat() throws Exception {
        String json = loadFixture("weibo_hottrend_response.json");
        List<HotItem> items = (List<HotItem>) parseWeiboResponse.invoke(fetcher, json);

        for (HotItem item : items) {
            assertNotNull(item.getId(), "ID 不应为 null");
            assertTrue(item.getId().startsWith("weibo_"),
                    "ID 应以 weibo_ 开头，实际: " + item.getId());
        }
    }

    @Test
    @DisplayName("API 返回 ok!=1 时应抛出 IOException")
    void testParseErrorResponse() {
        String json = "{\"ok\":0,\"data\":{\"realtime\":[]}}";
        Exception ex = assertThrows(Exception.class, () ->
                parseWeiboResponse.invoke(fetcher, json));
        assertTrue(ex.getCause() instanceof IOException, "应抛出 IOException");
        assertTrue(ex.getCause().getMessage().contains("ok=0"),
                "异常信息应包含 ok=0");
    }

    @Test
    @DisplayName("平台信息验证")
    void testPlatformInfo() {
        assertEquals("weibo", fetcher.platform());
        assertEquals("微博热搜", fetcher.platformCN());
    }
}
