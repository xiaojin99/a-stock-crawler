package com.stock.crawler.service;

import com.stock.crawler.model.Telegraph;
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
 * 财经快讯服务集成测试（需要网络，运行: mvn test -Dintegration=true）
 */
@Tag("integration")
@DisplayName("财经快讯服务集成测试")
class NewsServiceTest {

    private NewsService newsService;

    @BeforeEach
    void setUp() {
        newsService = new NewsService();
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("测试获取财联社快讯列表")
    void testGetTelegraphList() throws IOException {
        List<Telegraph> telegraphs = newsService.getTelegraphList();

        // 验证返回数据非空
        assertNotNull(telegraphs, "快讯列表不应为 null");
        assertFalse(telegraphs.isEmpty(), "快讯列表不应为空，请检查财联社页面结构是否变更");

        // 验证第一条数据字段
        Telegraph first = telegraphs.get(0);
        assertNotNull(first.getTime(), "时间字段不应为 null");
        assertNotNull(first.getContent(), "内容字段不应为 null");
        assertFalse(first.getContent().isEmpty(), "内容不应为空");

        System.out.println("=== 财联社快讯列表 ===");
        System.out.println("共获取 " + telegraphs.size() + " 条快讯");
        for (int i = 0; i < Math.min(5, telegraphs.size()); i++) {
            Telegraph t = telegraphs.get(i);
            System.out.printf("[%s] %s%n", t.getTime(), t.getContent());
            if (t.getUrl() != null && !t.getUrl().isEmpty()) {
                System.out.println("  详情: " + t.getUrl());
            }
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("测试获取最新一条快讯")
    void testGetLatestTelegraph() throws IOException {
        Telegraph latest = newsService.getLatestTelegraph();

        assertNotNull(latest, "最新快讯不应为 null");
        assertNotNull(latest.getContent(), "最新快讯内容不应为 null");

        System.out.println("=== 最新快讯 ===");
        System.out.printf("[%s] %s%n", latest.getTime(), latest.getContent());
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("测试缓存机制")
    void testCacheMechanism() throws IOException {
        // 第一次获取
        long start1 = System.currentTimeMillis();
        List<Telegraph> list1 = newsService.getTelegraphList();
        long time1 = System.currentTimeMillis() - start1;

        // 第二次获取（应该从缓存读取）
        long start2 = System.currentTimeMillis();
        List<Telegraph> list2 = newsService.getTelegraphList();
        long time2 = System.currentTimeMillis() - start2;

        // 验证数据一致性
        assertEquals(list1.size(), list2.size(), "缓存数据大小应一致");

        System.out.println("=== 缓存测试 ===");
        System.out.println("首次请求耗时: " + time1 + "ms");
        System.out.println("缓存请求耗时: " + time2 + "ms");
        System.out.println("缓存命中: " + (time2 < time1));
    }
}
