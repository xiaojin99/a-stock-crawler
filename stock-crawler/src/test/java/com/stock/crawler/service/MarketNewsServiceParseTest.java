package com.stock.crawler.service;

import com.stock.crawler.model.MarketNewsItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("市场快讯聚合解析测试")
class MarketNewsServiceParseTest {

    @Test
    @DisplayName("多源新闻应按真实发布时间倒序排列")
    @SuppressWarnings("unchecked")
    void mergeNewsSortsByParsedTimeAcrossSourceFormats() throws Exception {
        MarketNewsService service = new MarketNewsService();
        Method mergeMethod = MarketNewsService.class.getDeclaredMethod("mergeNews", List[].class);
        mergeMethod.setAccessible(true);

        List<MarketNewsItem> merged = (List<MarketNewsItem>) mergeMethod.invoke(service, (Object) new List[]{
                List.of(news("older", "2026-06-16T07:48:48")),
                List.of(news("newer", "2026-06-16 07:49:06")),
                List.of(news("invalid-time", ""))
        });

        assertEquals("newer", merged.get(0).getTitle());
        assertEquals("older", merged.get(1).getTitle());
        assertEquals("invalid-time", merged.get(2).getTitle());
    }

    private MarketNewsItem news(String title, String time) {
        return MarketNewsItem.builder()
                .title(title)
                .content(title)
                .time(time)
                .source("test")
                .build();
    }
}
