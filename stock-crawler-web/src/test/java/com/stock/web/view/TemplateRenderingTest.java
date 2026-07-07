package com.stock.web.view;

import com.stock.crawler.model.HotItem;
import com.stock.crawler.model.LongHuBangItem;
import com.stock.crawler.model.MarketHotBoard;
import com.stock.crawler.model.MarketHotItem;
import com.stock.crawler.model.MarketNewsItem;
import com.stock.crawler.model.StockRankItem;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateRenderingTest {

    private final SpringTemplateEngine templateEngine = createTemplateEngine();

    @Test
    void rankTemplateRendersItemsWithMissingMarketFields() {
        StockRankItem item = new StockRankItem();
        item.setRank(4);
        item.setStockCode("SH600519");
        item.setRankChange(null);

        Context context = new Context();
        context.setVariable("items", List.of(item));

        String html = assertDoesNotThrow(() -> templateEngine.process("rank", context));
        assertTrue(html.contains("600519"));
    }

    @Test
    void longHuBangTemplateRendersItemsWithMissingNumericFields() {
        LongHuBangItem item = new LongHuBangItem();
        item.setCode("600519");
        item.setName("贵州茅台");
        item.setChangePercent(null);
        item.setNetBuyAmt(null);

        Context context = new Context();
        context.setVariable("items", List.of(item));

        String html = assertDoesNotThrow(() -> templateEngine.process("longhubang", context));
        assertTrue(html.contains("贵州茅台"));
    }

    @Test
    void hotTemplateDoesNotRenderUnsafeExternalHref() {
        HotItem item = new HotItem("bad", "危险链接", "javascript:alert(1)", 1, "weibo");
        Context context = new Context();
        context.setVariable("weibo", List.of(item));
        context.setVariable("baidu", List.of());

        String html = templateEngine.process("hot", context);

        assertTrue(html.contains("危险链接"));
        assertFalse(html.contains("href=\"javascript:alert(1)\""));
    }

    @Test
    void marketNewsTemplateDoesNotRenderUnsafeExternalHref() {
        MarketNewsItem news = MarketNewsItem.builder()
                .title("危险资讯")
                .source("测试源")
                .url("data:text/html,<script>alert(1)</script>")
                .build();
        MarketHotBoard board = MarketHotBoard.builder()
                .boardId("test")
                .boardName("测试看板")
                .items(List.of(MarketHotItem.builder()
                        .rank(1)
                        .title("危险热榜")
                        .url("javascript:alert(1)")
                        .build()))
                .build();
        Context context = new Context();
        context.setVariable("keyword", "");
        context.setVariable("news", List.of(news));
        context.setVariable("boards", List.of(board));

        String html = templateEngine.process("market-news", context);

        assertTrue(html.contains("危险资讯"));
        assertTrue(html.contains("危险热榜"));
        assertFalse(html.contains("href=\"data:text/html"));
        assertFalse(html.contains("href=\"javascript:alert(1)\""));
    }

    private SpringTemplateEngine createTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
