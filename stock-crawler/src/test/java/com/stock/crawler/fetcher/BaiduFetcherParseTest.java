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
 * 百度热搜 JSON 解析逻辑单元测试（不依赖网络）
 */
@DisplayName("百度热搜解析单元测试")
class BaiduFetcherParseTest {

    private BaiduFetcher fetcher;
    private Method parseBaiduResponse;

    @BeforeEach
    void setUp() throws Exception {
        fetcher = new BaiduFetcher();
        parseBaiduResponse = BaiduFetcher.class
                .getDeclaredMethod("parseBaiduResponse", String.class);
        parseBaiduResponse.setAccessible(true);
    }

    private String loadFixture(String name) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/fixtures/" + name)) {
            assertNotNull(is, "Fixture file not found: " + name);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("正常响应解析 - 验证嵌套 content.content 结构")
    @SuppressWarnings("unchecked")
    void testParseNormalResponse() throws Exception {
        String json = loadFixture("baidu_hottrend_response.json");
        List<HotItem> items = (List<HotItem>) parseBaiduResponse.invoke(fetcher, json);

        assertNotNull(items, "列表不应为 null");
        assertFalse(items.isEmpty(), "列表不应为空");
        assertEquals(5, items.size(), "应解析出 5 条热搜");
    }

    @Test
    @DisplayName("第一条热搜字段验证")
    @SuppressWarnings("unchecked")
    void testFirstItemFields() throws Exception {
        String json = loadFixture("baidu_hottrend_response.json");
        List<HotItem> items = (List<HotItem>) parseBaiduResponse.invoke(fetcher, json);

        HotItem first = items.get(0);
        assertEquals("人工智能大模型新进展", first.getTitle(), "标题应正确");
        assertEquals(1, first.getRank(), "第一条排名应为 1");
        assertEquals("baidu", first.getPlatform(), "平台应为 baidu");
        assertNotNull(first.getUrl(), "URL 不应为 null");
        assertFalse(first.getUrl().isEmpty(), "URL 不应为空");
    }

    @Test
    @DisplayName("排名连续性验证 - 从 1 开始连续递增")
    @SuppressWarnings("unchecked")
    void testRankSequential() throws Exception {
        String json = loadFixture("baidu_hottrend_response.json");
        List<HotItem> items = (List<HotItem>) parseBaiduResponse.invoke(fetcher, json);

        for (int i = 0; i < items.size(); i++) {
            assertEquals(i + 1, items.get(i).getRank(),
                    "第 " + (i + 1) + " 条排名应为 " + (i + 1));
        }
    }

    @Test
    @DisplayName("ID 格式验证 - 应以 baidu_ 开头")
    @SuppressWarnings("unchecked")
    void testIdFormat() throws Exception {
        String json = loadFixture("baidu_hottrend_response.json");
        List<HotItem> items = (List<HotItem>) parseBaiduResponse.invoke(fetcher, json);

        for (HotItem item : items) {
            assertNotNull(item.getId(), "ID 不应为 null");
            assertTrue(item.getId().startsWith("baidu_"),
                    "ID 应以 baidu_ 开头，实际: " + item.getId());
        }
    }

    @Test
    @DisplayName("空 cards 数组应返回空列表")
    @SuppressWarnings("unchecked")
    void testParseEmptyCards() throws Exception {
        String json = "{\"data\":{\"cards\":[]}}";
        List<HotItem> items = (List<HotItem>) parseBaiduResponse.invoke(fetcher, json);
        assertNotNull(items);
        assertTrue(items.isEmpty(), "空 cards 应返回空列表");
    }

    @Test
    @DisplayName("平台信息验证")
    void testPlatformInfo() {
        assertEquals("baidu", fetcher.platform());
        assertEquals("百度热搜", fetcher.platformCN());
    }
}
