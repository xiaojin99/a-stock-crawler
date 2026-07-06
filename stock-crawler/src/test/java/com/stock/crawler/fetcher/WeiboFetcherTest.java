package com.stock.crawler.fetcher;

import com.stock.crawler.model.HotItem;
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
 * 微博热搜获取器集成测试（需要网络，运行: mvn test -Dintegration=true）
 */
@Tag("integration")
@DisplayName("微博热搜获取器集成测试")
class WeiboFetcherTest {

    private WeiboFetcher weiboFetcher;

    @BeforeEach
    void setUp() {
        weiboFetcher = new WeiboFetcher();
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("测试获取微博热搜列表")
    void testFetch() throws IOException {
        List<HotItem> items = weiboFetcher.fetch();

        // 验证返回数据非空
        assertNotNull(items, "热搜列表不应为 null");
        assertFalse(items.isEmpty(), "热搜列表不应为空");

        // 验证第一条数据
        HotItem first = items.get(0);
        assertNotNull(first.getTitle(), "热搜标题不应为 null");
        assertFalse(first.getTitle().isEmpty(), "热搜标题不应为空");
        assertEquals(1, first.getRank(), "第一条热搜排名应为 1");
        assertEquals("weibo", first.getPlatform(), "平台应为 weibo");

        System.out.println("=== 微博热搜列表 ===");
        System.out.println("共获取 " + items.size() + " 条热搜");
        for (int i = 0; i < Math.min(10, items.size()); i++) {
            HotItem item = items.get(i);
            System.out.printf("%d. %s (热度: %s)%n",
                    item.getRank(),
                    item.getTitle(),
                    item.getHotScore() != null ? item.getHotScore() : "N/A");
        }
    }

    @Test
    @DisplayName("测试平台信息")
    void testPlatformInfo() {
        assertEquals("weibo", weiboFetcher.platform());
        assertEquals("微博热搜", weiboFetcher.platformCN());

        System.out.println("=== 平台信息 ===");
        System.out.println("平台ID: " + weiboFetcher.platform());
        System.out.println("平台名称: " + weiboFetcher.platformCN());
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("测试热搜数据完整性")
    void testDataIntegrity() throws IOException {
        List<HotItem> items = weiboFetcher.fetch();

        for (int i = 0; i < items.size(); i++) {
            HotItem item = items.get(i);

            // 验证必要字段
            assertNotNull(item.getId(), "ID 不应为 null");
            assertNotNull(item.getTitle(), "标题不应为 null");
            assertNotNull(item.getUrl(), "URL 不应为 null");
            assertEquals(i + 1, item.getRank(), "排名应正确");

            // ID 格式验证
            assertTrue(item.getId().startsWith("weibo_"), "ID 应以 'weibo_' 开头");
        }

        System.out.println("=== 数据完整性测试 ===");
        System.out.println("所有 " + items.size() + " 条数据验证通过");
    }
}
