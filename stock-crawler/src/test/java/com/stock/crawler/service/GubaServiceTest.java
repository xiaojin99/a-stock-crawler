package com.stock.crawler.service;

import com.stock.crawler.model.GubaComment;
import com.stock.crawler.model.GubaPost;
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
 * 股吧爬虫服务集成测试（需要网络，运行: mvn test -Dintegration=true）
 */
@Tag("integration")
@DisplayName("股吧爬虫服务集成测试")
class GubaServiceTest {

    private GubaService gubaService;

    @BeforeEach
    void setUp() {
        gubaService = new GubaService();
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("测试获取平安銀行股吧帖子列表")
    void testGetPostList() throws IOException {
        // 平安银行
        String stockCode = "000001";
        List<GubaPost> posts = gubaService.getPostList(stockCode, 20, 1);

        // 验证返回数据非空
        assertNotNull(posts, "帖子列表不应为 null");
        assertFalse(posts.isEmpty(), "帖子列表不应为空");

        System.out.println("=== 平安银行股吧帖子列表 ===");
        System.out.println("共获取 " + posts.size() + " 条帖子");
        System.out.println();

        for (int i = 0; i < Math.min(10, posts.size()); i++) {
            GubaPost post = posts.get(i);
            System.out.printf("%d. %s%n", i + 1, post.getTitle());
            System.out.printf("   作者: %s | 阅读: %s | 评论: %s%n",
                    post.getAuthor() != null ? post.getAuthor() : "未知",
                    post.getReadCount() != null ? post.getReadCount() : "N/A",
                    post.getCommentCount() != null ? post.getCommentCount() : "N/A");
            if (post.getIsTop() != null && post.getIsTop()) {
                System.out.println("   [置顶]");
            }
            if (post.getIsEssence() != null && post.getIsEssence()) {
                System.out.println("   [精华]");
            }
            System.out.println();
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("测试获取贵州茅台股吧帖子")
    void testGetMaoTaiPosts() throws IOException {
        // 贵州茅台
        String stockCode = "600519";
        List<GubaPost> posts = gubaService.getPostList(stockCode, 10, 1);

        assertNotNull(posts, "帖子列表不应为 null");

        System.out.println("=== 贵州茅台股吧帖子 ===");
        System.out.println("共获取 " + posts.size() + " 条帖子");

        for (GubaPost post : posts) {
            System.out.printf("- %s (阅读: %s)%n",
                    post.getTitle(),
                    post.getReadCount() != null ? post.getReadCount() : "N/A");
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("测试帖子数据完整性")
    void testPostDataIntegrity() throws IOException {
        List<GubaPost> posts = gubaService.getPostList("000001", 10, 1);

        for (GubaPost post : posts) {
            // 验证必要字段
            assertNotNull(post.getPostId(), "帖子ID不应为 null");
            assertNotNull(post.getTitle(), "帖子标题不应为 null");
            assertFalse(post.getTitle().isEmpty(), "帖子标题不应为空");
            assertNotNull(post.getUrl(), "帖子URL不应为 null");

            // URL 格式验证
            assertTrue(post.getUrl().contains("guba.eastmoney.com"),
                    "URL 应包含 guba.eastmoney.com");
        }

        System.out.println("=== 数据完整性测试 ===");
        System.out.println("所有 " + posts.size() + " 条帖子数据验证通过");
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    @DisplayName("测试获取帖子详情")
    void testGetPostDetail() throws IOException {
        // 先获取帖子列表
        List<GubaPost> posts = gubaService.getPostList("000001", 5, 1);

        if (posts.isEmpty()) {
            System.out.println("帖子列表为空，跳过详情测试");
            return;
        }

        // 获取第一条非置顶帖子的详情
        GubaPost firstPost = posts.stream()
                .filter(p -> p.getIsTop() == null || !p.getIsTop())
                .findFirst()
                .orElse(posts.get(0));

        GubaPost detail = gubaService.getPostDetail(firstPost.getStockCode(), firstPost.getPostId());

        assertNotNull(detail, "帖子详情不应为 null");
        assertNotNull(detail.getContent(), "帖子内容不应为 null");

        System.out.println("=== 帖子详情 ===");
        System.out.println("标题: " + detail.getTitle());
        System.out.println("作者: " + detail.getAuthor());
        System.out.println("内容: " + (detail.getContent().length() > 200 ?
                detail.getContent().substring(0, 200) + "..." : detail.getContent()));
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    @DisplayName("测试获取帖子评论")
    void testGetComments() throws IOException {
        // 先获取帖子列表
        List<GubaPost> posts = gubaService.getPostList("000001", 5, 1);

        if (posts.isEmpty()) {
            System.out.println("帖子列表为空，跳过评论测试");
            return;
        }

        // 找一条有评论的帖子
        GubaPost postWithComments = posts.stream()
                .filter(p -> p.getCommentCount() != null && p.getCommentCount() > 0)
                .findFirst()
                .orElse(null);

        if (postWithComments == null) {
            System.out.println("没有找到有评论的帖子，跳过测试");
            return;
        }

        List<GubaComment> comments = gubaService.getComments(postWithComments.getPostId(), 10, 1);

        assertNotNull(comments, "评论列表不应为 null");

        System.out.println("=== 帖子评论 ===");
        System.out.println("帖子: " + postWithComments.getTitle());
        System.out.println("评论数: " + comments.size());
        System.out.println();

        for (int i = 0; i < Math.min(5, comments.size()); i++) {
            GubaComment comment = comments.get(i);
            System.out.printf("%d. %s: %s%n",
                    i + 1,
                    comment.getAuthor() != null ? comment.getAuthor() : "匿名",
                    comment.getContent() != null && comment.getContent().length() > 50 ?
                            comment.getContent().substring(0, 50) + "..." : comment.getContent());
            if (comment.getLikeCount() != null && comment.getLikeCount() > 0) {
                System.out.printf("   👍 %d%n", comment.getLikeCount());
            }
        }
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    @DisplayName("测试不同股票代码")
    void testDifferentStockCodes() throws IOException {
        String[] codes = {"000001", "600519", "000858", "601318"};

        System.out.println("=== 不同股票代码测试 ===");
        for (String code : codes) {
            List<GubaPost> posts = gubaService.getPostList(code, 5, 1);
            System.out.printf("%s: %d 条帖子%n", code, posts.size());
            assertFalse(posts == null, "帖子列表不应为 null");
        }
    }
}
