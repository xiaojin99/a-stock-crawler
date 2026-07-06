package com.stock.crawler.service;

import com.stock.crawler.model.GubaPost;
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
 * 股吧 HTML 解析逻辑单元测试（不依赖网络）
 */
@DisplayName("股吧解析单元测试")
class GubaServiceParseTest {

    private GubaService service;
    private Method parseEmbeddedJson;

    @BeforeEach
    void setUp() throws Exception {
        service = new GubaService();
        parseEmbeddedJson = GubaService.class
                .getDeclaredMethod("parseEmbeddedJson", String.class, String.class);
        parseEmbeddedJson.setAccessible(true);
    }

    private String loadFixture(String name) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/fixtures/" + name)) {
            assertNotNull(is, "Fixture file not found: " + name);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("正常 HTML 解析 - 验证帖子数量和字段")
    @SuppressWarnings("unchecked")
    void testParseNormalHtml() throws Exception {
        String html = loadFixture("guba_list.html");
        List<GubaPost> posts = (List<GubaPost>) parseEmbeddedJson.invoke(service, html, "000001");

        assertNotNull(posts, "帖子列表不应为 null");
        assertFalse(posts.isEmpty(), "帖子列表不应为空");
        assertEquals(5, posts.size(), "应解析出 5 条帖子");
    }

    @Test
    @DisplayName("第一条帖子字段验证")
    @SuppressWarnings("unchecked")
    void testFirstPostFields() throws Exception {
        String html = loadFixture("guba_list.html");
        List<GubaPost> posts = (List<GubaPost>) parseEmbeddedJson.invoke(service, html, "000001");

        GubaPost first = posts.get(0);
        assertEquals("1684774522", first.getPostId(), "帖子 ID 应正确");
        assertEquals("平安银行年报超预期，分红比例提升至30%", first.getTitle(), "标题应正确");
        assertEquals("股海老司机", first.getAuthor(), "作者应正确");
        assertEquals(15234, (int) first.getReadCount(), "阅读量应正确");
        assertEquals(88, (int) first.getCommentCount(), "评论数应正确");
        assertFalse(first.getIsTop(), "第一条不应是置顶帖");
        assertEquals("000001", first.getStockCode(), "股票代码应正确");
    }

    @Test
    @DisplayName("置顶帖验证")
    @SuppressWarnings("unchecked")
    void testTopPost() throws Exception {
        String html = loadFixture("guba_list.html");
        List<GubaPost> posts = (List<GubaPost>) parseEmbeddedJson.invoke(service, html, "000001");

        GubaPost topPost = posts.get(1);
        assertEquals("1684774523", topPost.getPostId());
        assertTrue(topPost.getIsTop(), "第二条应是置顶帖");
        assertEquals("管理员", topPost.getAuthor());
    }

    @Test
    @DisplayName("帖子 URL 构建验证")
    @SuppressWarnings("unchecked")
    void testPostUrlBuilding() throws Exception {
        String html = loadFixture("guba_list.html");
        List<GubaPost> posts = (List<GubaPost>) parseEmbeddedJson.invoke(service, html, "000001");

        for (GubaPost post : posts) {
            assertNotNull(post.getUrl(), "URL 不应为 null");
            assertTrue(post.getUrl().contains("guba.eastmoney.com"), "URL 应包含域名");
            assertTrue(post.getUrl().contains(post.getPostId()), "URL 应包含帖子 ID");
            assertTrue(post.getUrl().contains("000001"), "URL 应包含股票代码");
        }
    }

    @Test
    @DisplayName("时间解析验证")
    @SuppressWarnings("unchecked")
    void testTimeParser() throws Exception {
        String html = loadFixture("guba_list.html");
        List<GubaPost> posts = (List<GubaPost>) parseEmbeddedJson.invoke(service, html, "000001");

        // 第一条 post_publish_time="2026-03-25 10:30:00"
        GubaPost first = posts.get(0);
        assertNotNull(first.getPostTime(), "发帖时间不应为 null");
        assertEquals(2026, first.getPostTime().getYear(), "年份应为 2026");
        assertEquals(3, first.getPostTime().getMonthValue(), "月份应为 3");
        assertEquals(25, first.getPostTime().getDayOfMonth(), "日期应为 25");
        assertEquals(10, first.getPostTime().getHour(), "小时应为 10");
        assertEquals(30, first.getPostTime().getMinute(), "分钟应为 30");
    }

    @Test
    @DisplayName("找不到 article_list 应抛出 IOException")
    void testMissingArticleList() {
        String html = "<html><body><p>No stock data here</p></body></html>";
        Exception ex = assertThrows(Exception.class, () ->
                parseEmbeddedJson.invoke(service, html, "000001"));
        assertTrue(ex.getCause() instanceof IOException, "应抛出 IOException");
        assertTrue(ex.getCause().getMessage().contains("article_list"),
                "异常信息应提到 article_list");
    }
}
