package com.stock.crawler.service;

import com.stock.crawler.model.LongHuBangDetail;
import com.stock.crawler.model.LongHuBangItem;
import com.stock.crawler.service.LongHuBangService.LongHuBangResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 龙虎榜服务集成测试（需要网络，运行: mvn test -Dintegration=true）
 */
@Tag("integration")
@DisplayName("龙虎榜服务集成测试")
class LongHuBangServiceTest {

    private LongHuBangService longHuBangService;

    @BeforeEach
    void setUp() {
        longHuBangService = new LongHuBangService();
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("测试获取龙虎榜列表")
    void testGetLongHuBangList() throws IOException {
        LongHuBangResult result = longHuBangService.getLongHuBangList();

        assertNotNull(result, "结果不应为 null");
        assertNotNull(result.getItems(), "列表不应为 null");
        // 非交易日可能无数据，使用 assumeTrue 跳过后续断言
        assumeTrue(!result.getItems().isEmpty(), "当日无龙虎榜数据（可能是非交易日），跳过测试");

        List<LongHuBangItem> items = result.getItems();

        System.out.println("=== 龙虎榜列表 ===");
        System.out.println("共获取 " + items.size() + " 条记录，总数: " + result.getTotal());
        for (int i = 0; i < Math.min(10, items.size()); i++) {
            LongHuBangItem item = items.get(i);
            System.out.printf("%d. [%s] %s 涨幅: %.2f%% 净买入: %.2f万%n",
                    i + 1,
                    item.getCode(),
                    item.getName(),
                    item.getChangePercent(),
                    item.getNetBuyAmt() / 10000);
            System.out.printf("   上榜原因: %s%n", item.getReason());
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("测试按日期获取龙虎榜")
    void testGetLongHuBangListByDate() throws IOException {
        // 获取最近的交易日（默认使用今天）
        LongHuBangResult result = longHuBangService.getLongHuBangList(50, 1, null);

        System.out.println("=== 按日期获取龙虎榜 ===");
        System.out.println("返回数量: " + result.getItems().size());
        System.out.println("总数: " + result.getTotal());

        // 如果当天有数据，验证数据完整性
        if (!result.getItems().isEmpty()) {
            LongHuBangItem firstItem = result.getItems().get(0);
            assertNotNull(firstItem.getTradeDate(), "交易日期不应为 null");
            System.out.println("交易日期: " + firstItem.getTradeDate());
        } else {
            System.out.println("当日暂无龙虎榜数据（可能是非交易日）");
        }
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    @DisplayName("测试分页获取龙虎榜")
    void testGetLongHuBangListWithPaging() throws IOException {
        LongHuBangResult result1 = longHuBangService.getLongHuBangList(10, 1, null);
        assumeTrue(!result1.getItems().isEmpty(), "当日无龙虎榜数据，跳过分页测试");

        LongHuBangResult result2 = longHuBangService.getLongHuBangList(10, 2, null);

        assertNotNull(result1, "第一页结果不应为 null");
        assertNotNull(result2, "第二页结果不应为 null");
        assertEquals(10, result1.getItems().size(), "每页应返回 10 条");
        assertEquals(10, result2.getItems().size(), "第二页也应返回 10 条");

        // 验证两页数据不同
        assertNotEquals(
                result1.getItems().get(0).getCode(),
                result2.getItems().get(0).getCode(),
                "两页数据应不同"
        );

        System.out.println("=== 分页测试 ===");
        System.out.println("第1页: " + result1.getItems().size() + " 条");
        System.out.println("第2页: " + result2.getItems().size() + " 条");
        System.out.println("总数: " + result1.getTotal());
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("测试龙虎榜数据字段完整性")
    void testDataIntegrity() throws IOException {
        LongHuBangResult result = longHuBangService.getLongHuBangList(20, 1, null);
        assumeTrue(!result.getItems().isEmpty(), "当日无龙虎榜数据，跳过完整性测试");

        for (LongHuBangItem item : result.getItems()) {
            // 验证必要字段
            assertNotNull(item.getTradeDate(), "交易日期不应为 null");
            assertNotNull(item.getCode(), "股票代码不应为 null");
            assertNotNull(item.getName(), "股票名称不应为 null");

            // 日期格式验证
            assertTrue(item.getTradeDate().matches("\\d{4}-\\d{2}-\\d{2}"),
                    "日期格式应为 YYYY-MM-DD");

            // 代码格式验证
            assertTrue(item.getCode().matches("\\d{6}"),
                    "股票代码应为 6 位数字");
        }

        System.out.println("=== 数据完整性测试 ===");
        System.out.println("所有 " + result.getItems().size() + " 条记录验证通过");
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    @DisplayName("测试获取个股龙虎榜明细")
    void testGetStockDetail() throws IOException {
        // 先获取龙虎榜列表
        LongHuBangResult listResult = longHuBangService.getLongHuBangList(5, 1, null);

        if (listResult.getItems().isEmpty()) {
            System.out.println("龙虎榜列表为空，跳过明细测试");
            return;
        }

        // 获取第一条记录的明细
        LongHuBangItem first = listResult.getItems().get(0);
        List<LongHuBangDetail> details = longHuBangService.getStockDetail(
                first.getCode(),
                first.getTradeDate()
        );

        System.out.println("=== 个股龙虎榜明细 ===");
        System.out.printf("股票: %s (%s)%n", first.getName(), first.getCode());
        System.out.printf("日期: %s%n", first.getTradeDate());
        System.out.println("营业部明细: " + details.size() + " 条");

        for (LongHuBangDetail detail : details) {
            System.out.printf("[%s] %s%n", detail.getDirection(), detail.getOperName());
            System.out.printf("  买入: %.2f万  卖出: %.2f万  净额: %.2f万%n",
                    detail.getBuyAmt() / 10000,
                    detail.getSellAmt() / 10000,
                    detail.getNetAmt() / 10000);
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("测试缓存机制")
    void testCacheMechanism() throws IOException {
        // 第一次获取
        long start1 = System.currentTimeMillis();
        LongHuBangResult result1 = longHuBangService.getLongHuBangList();
        long time1 = System.currentTimeMillis() - start1;

        // 第二次获取（应该从缓存读取）
        long start2 = System.currentTimeMillis();
        LongHuBangResult result2 = longHuBangService.getLongHuBangList();
        long time2 = System.currentTimeMillis() - start2;

        // 验证数据一致性
        assertEquals(result1.getItems().size(), result2.getItems().size(), "缓存数据大小应一致");

        System.out.println("=== 缓存测试 ===");
        System.out.println("首次请求耗时: " + time1 + "ms");
        System.out.println("缓存请求耗时: " + time2 + "ms");
    }
}
