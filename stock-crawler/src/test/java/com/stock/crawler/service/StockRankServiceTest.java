package com.stock.crawler.service;

import com.stock.crawler.model.StockRankItem;
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
 * 个股人气榜服务集成测试（需要网络，运行: mvn test -Dintegration=true）
 */
@Tag("integration")
@DisplayName("个股人气榜服务集成测试")
class StockRankServiceTest {

    private StockRankService stockRankService;

    @BeforeEach
    void setUp() {
        stockRankService = new StockRankService();
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("测试获取人气榜列表")
    void testGetRankList() throws IOException {
        List<StockRankItem> items = stockRankService.getRankList(50);

        assertNotNull(items, "人气榜列表不应为 null");
        assertFalse(items.isEmpty(), "人气榜列表不应为空");

        System.out.println("=== 个股人气榜 Top 50 ===");
        System.out.println("排名\t代码\t\t名称\t\t\t涨跌幅\t排名变化");
        System.out.println("─".repeat(60));

        for (StockRankItem item : items) {
            String changeStr = String.format("%.2f%%", item.getChangePercent() != null ? item.getChangePercent() : 0);
            String rankChangeStr = switch (item.getRankChange()) {
                case 1 -> "↑";
                case -1 -> "↓";
                default -> "-";
            };

            System.out.printf("%d\t%s\t%s\t\t%s\t%s%n",
                    item.getRank(),
                    item.getStockCode(),
                    item.getStockName() != null ? item.getStockName() : "未知",
                    changeStr,
                    rankChangeStr);
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("测试获取 Top 10")
    void testGetTop10() throws IOException {
        List<StockRankItem> items = stockRankService.getRankList(10);

        assertFalse(items.isEmpty(), "人气榜不应为空");
        // API 可能返回少于请求数量，这里验证不超过 10
        assertTrue(items.size() <= 10, "返回数量不应超过请求的数量");
        assertTrue(items.size() > 0, "应至少返回 1 条数据");

        System.out.println("=== 人气榜 Top 10 ===");
        for (int i = 0; i < items.size(); i++) {
            StockRankItem item = items.get(i);
            assertEquals(i + 1, item.getRank(), "排名应该从 1 开始");
            System.out.printf("%d. %s (%s) - %s%n",
                    item.getRank(),
                    item.getStockName() != null ? item.getStockName() : item.getStockCode(),
                    item.getStockCode(),
                    item.getRankChangeDesc());
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("测试数据完整性")
    void testDataIntegrity() throws IOException {
        List<StockRankItem> items = stockRankService.getRankList(20);

        for (StockRankItem item : items) {
            assertNotNull(item.getStockCode(), "股票代码不应为 null");
            assertNotNull(item.getRank(), "排名不应为 null");
            assertTrue(item.getRank() > 0, "排名应该大于 0");
            assertNotNull(item.getPureCode(), "纯代码不应为 null");
        }

        System.out.println("=== 数据完整性测试 ===");
        System.out.println("所有 " + items.size() + " 条数据验证通过");
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    @DisplayName("测试不同数量")
    void testDifferentPageSize() throws IOException {
        int[] sizes = {5, 10, 30, 100};

        for (int size : sizes) {
            List<StockRankItem> items = stockRankService.getRankList(size);
            assertTrue(items.size() <= size, "返回数量不应超过请求的数量");
            System.out.printf("请求 %d 条, 实际返回 %d 条%n", size, items.size());
        }
    }
}
