package com.stock.crawler.service;

import com.stock.crawler.model.HotItem;
import com.stock.crawler.model.HotTrendResult;
import com.stock.crawler.service.HotTrendService.PlatformInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 舆情热点服务集成测试（需要网络，运行: mvn test -Dintegration=true）
 */
@Tag("integration")
@DisplayName("舆情热点服务集成测试")
class HotTrendServiceTest {

    private HotTrendService hotTrendService;

    @BeforeEach
    void setUp() {
        hotTrendService = new HotTrendService();
    }

    @Test
    @DisplayName("测试获取支持的平台列表")
    void testGetPlatforms() {
        List<PlatformInfo> platforms = hotTrendService.getPlatforms();

        assertNotNull(platforms, "平台列表不应为 null");
        assertFalse(platforms.isEmpty(), "平台列表不应为空");

        System.out.println("=== 支持的平台 ===");
        for (PlatformInfo info : platforms) {
            System.out.printf("- %s (%s)%n", info.name(), info.id());
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("测试获取微博热搜")
    void testGetWeiboHotTrend() {
        HotTrendResult result = hotTrendService.getHotTrend("weibo");

        assertNotNull(result, "结果不应为 null");
        assertEquals("weibo", result.getPlatform(), "平台应为 weibo");
        assertEquals("微博热搜", result.getPlatformCN(), "平台名称应为 微博热搜");
        assertFalse(result.hasError(), "不应有错误");

        List<HotItem> items = result.getItems();
        assertNotNull(items, "热搜列表不应为 null");
        assertFalse(items.isEmpty(), "热搜列表不应为空");

        System.out.println("=== 微博热搜 ===");
        System.out.println("共获取 " + items.size() + " 条");
        for (int i = 0; i < Math.min(5, items.size()); i++) {
            System.out.println(items.get(i));
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("测试获取百度热搜")
    void testGetBaiduHotTrend() {
        HotTrendResult result = hotTrendService.getHotTrend("baidu");

        assertNotNull(result, "结果不应为 null");
        assertEquals("baidu", result.getPlatform(), "平台应为 baidu");
        assertEquals("百度热搜", result.getPlatformCN(), "平台名称应为 百度热搜");
        assertFalse(result.hasError(), "不应有错误");

        List<HotItem> items = result.getItems();
        assertNotNull(items, "热搜列表不应为 null");
        assertFalse(items.isEmpty(), "热搜列表不应为空");

        System.out.println("=== 百度热搜 ===");
        System.out.println("共获取 " + items.size() + " 条");
        for (int i = 0; i < Math.min(5, items.size()); i++) {
            System.out.println(items.get(i));
        }
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    @DisplayName("测试并发获取所有平台热点")
    void testGetAllHotTrends() {
        List<HotTrendResult> results = hotTrendService.getAllHotTrends();

        assertNotNull(results, "结果列表不应为 null");
        assertFalse(results.isEmpty(), "结果列表不应为空");

        System.out.println("=== 所有平台热点 ===");
        for (HotTrendResult result : results) {
            System.out.printf("%n--- %s ---%n", result.getPlatformCN());
            if (result.hasError()) {
                System.out.println("错误: " + result.getError());
            } else {
                System.out.println("共获取 " + result.getItems().size() + " 条热点");
                for (int i = 0; i < Math.min(3, result.getItems().size()); i++) {
                    System.out.println(result.getItems().get(i));
                }
            }
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("测试缓存机制")
    void testCacheMechanism() {
        // 第一次获取
        long start1 = System.currentTimeMillis();
        HotTrendResult result1 = hotTrendService.getHotTrend("weibo");
        long time1 = System.currentTimeMillis() - start1;

        assertFalse(result1.isFromCache(), "首次获取不应来自缓存");

        // 第二次获取（应该从缓存读取）
        long start2 = System.currentTimeMillis();
        HotTrendResult result2 = hotTrendService.getHotTrend("weibo");
        long time2 = System.currentTimeMillis() - start2;

        assertTrue(result2.isFromCache(), "第二次获取应来自缓存");
        assertEquals(result1.getItems().size(), result2.getItems().size(), "缓存数据大小应一致");

        System.out.println("=== 缓存测试 ===");
        System.out.println("首次请求耗时: " + time1 + "ms (fromCache: " + result1.isFromCache() + ")");
        System.out.println("缓存请求耗时: " + time2 + "ms (fromCache: " + result2.isFromCache() + ")");
    }

    @Test
    @DisplayName("测试不支持的平台")
    void testUnsupportedPlatform() {
        HotTrendResult result = hotTrendService.getHotTrend("unknown");

        assertNotNull(result, "结果不应为 null");
        assertTrue(result.hasError(), "应有错误");

        System.out.println("=== 不支持平台测试 ===");
        System.out.println("错误信息: " + result.getError());
    }
}
