package com.stock.crawler.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.crawler.model.GubaComment;
import com.stock.crawler.model.GubaPost;
import com.stock.crawler.util.CrawlerRequestPolicy;
import com.stock.crawler.util.HttpUtils;
import com.stock.crawler.util.StockCodeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 股吧爬虫服务
 * 抓取东方财富股吧的帖子和评论数据
 */
public class GubaService {

    private static final Logger log = LoggerFactory.getLogger(GubaService.class);

    // 股吧帖子列表页面 URL (数据嵌入在 HTML 中)
    private static final String GUBA_LIST_URL = "https://guba.eastmoney.com/list,%s.html";
    // 帖子详情页面
    private static final String GUBA_POST_URL = "https://guba.eastmoney.com/news,%s,%s.html";
    // 评论列表 CDN API
    private static final String GUBA_COMMENT_API = "https://gbcdn.dfcfw.com/gbapi/reply_api_Reply_ArticleNewReplyList.js" +
            "?code=0&postid=%s&sort=1&sorttype=1&ps=%d&p=%d";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 获取股吧帖子列表
     *
     * @param stockCode 股票代码 (如 "000001")
     * @param pageSize  每页数量 (用于限制返回数量)
     * @param page      页码 (当前只支持第一页)
     * @return 帖子列表
     */
    public List<GubaPost> getPostList(String stockCode, int pageSize, int page) throws IOException {
        String code = StockCodeUtils.stripMarket(stockCode);
        int safePageSize = StockCodeUtils.clamp(pageSize, 1, 100);
        String url = String.format(GUBA_LIST_URL, code);
        log.info("Fetching guba posts from: {}", url);

        Map<String, String> headers = Map.of(
                "Referer", "https://guba.eastmoney.com/"
        );

        String html = HttpUtils.getEastMoney(
                url, headers, CrawlerRequestPolicy.backgroundNews());
        List<GubaPost> posts = parseEmbeddedJson(html, code);

        // 限制返回数量
        if (posts.size() > safePageSize) {
            posts = posts.subList(0, safePageSize);
        }

        log.info("Got {} guba posts", posts.size());
        return posts;
    }

    /**
     * 获取股吧帖子列表（默认 20 条）
     */
    public List<GubaPost> getPostList(String stockCode) throws IOException {
        return getPostList(stockCode, 20, 1);
    }

    /**
     * 从 HTML 中提取嵌入的 JSON 数据
     * 数据格式: var article_list={"re":[...post data...]}
     */
    private List<GubaPost> parseEmbeddedJson(String html, String stockCode) throws IOException {
        List<GubaPost> posts = new ArrayList<>();

        // 找到 article_list JSON 的起始位置
        int startIndex = html.indexOf("var article_list=");
        if (startIndex == -1) {
            log.warn("Cannot find 'var article_list=' in guba HTML for stock: {}, HTML length: {}", stockCode, html.length());
            throw new IOException(
                "Cannot find article_list in guba page for stock: " + stockCode +
                ". Page structure may have changed.");
        }

        // 从等号后开始提取 JSON
        startIndex = html.indexOf("{", startIndex);
        if (startIndex == -1) {
            log.warn("Cannot find JSON start in article_list for stock: {}", stockCode);
            throw new IOException("Cannot find JSON start in article_list for stock: " + stockCode);
        }

        // 使用括号匹配来找到 JSON 结束位置
        int braceCount = 0;
        int endIndex = startIndex;
        boolean inString = false;
        boolean escape = false;

        for (int i = startIndex; i < html.length(); i++) {
            char c = html.charAt(i);

            if (escape) {
                escape = false;
                continue;
            }

            if (c == '\\' && inString) {
                escape = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (!inString) {
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        endIndex = i + 1;
                        break;
                    }
                }
            }
        }

        String jsonStr = html.substring(startIndex, endIndex);

        try {
            JsonNode root = objectMapper.readTree(jsonStr);
            JsonNode listNode = root.path("re");

            if (listNode.isArray()) {
                for (JsonNode node : listNode) {
                    GubaPost post = new GubaPost();
                    post.setPostId(textValue(node, "post_id"));
                    post.setStockCode(stockCode);
                    post.setTitle(textValue(node, "post_title"));
                    post.setContent(textValue(node, "post_content"));
                    post.setAuthor(textValue(node, "user_nickname"));
                    post.setAuthorId(textValue(node, "user_id"));
                    post.setReadCount(node.path("post_click_count").asInt(0));
                    post.setCommentCount(node.path("post_comment_count").asInt(0));
                    post.setLikeCount(node.path("post_like_count").asInt(0));
                    post.setIsTop(node.path("post_top_status").asInt(0) == 1);
                    post.setIsEssence(false);

                    // 解析时间
                    String postTimeStr = textValue(node, "post_publish_time");
                    if (postTimeStr != null) {
                        try {
                            post.setPostTime(LocalDateTime.parse(postTimeStr, DATE_FORMATTER));
                        } catch (Exception e) {
                            // 忽略时间解析错误
                        }
                    }

                    // 构建帖子 URL
                    if (post.getPostId() != null) {
                        post.setUrl(String.format(GUBA_POST_URL, stockCode, post.getPostId()));
                    }

                    posts.add(post);
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to parse embedded guba JSON for stock: " + stockCode, e);
        }

        log.info("Parsed {} posts from embedded JSON", posts.size());
        return posts;
    }

    /**
     * 获取帖子详情（包含内容）
     */
    public GubaPost getPostDetail(String stockCode, String postId) throws IOException {
        String url = String.format(GUBA_POST_URL, stockCode, postId);
        log.info("Fetching post detail: {}", url);

        String html = HttpUtils.getEastMoney(
                url,
                Map.of("Referer", "https://guba.eastmoney.com/"),
                CrawlerRequestPolicy.backgroundNews());
        return parsePostDetail(html, stockCode, postId);
    }

    /**
     * 解析帖子详情页面
     */
    private GubaPost parsePostDetail(String html, String stockCode, String postId) {
        Document doc = Jsoup.parse(html);
        GubaPost post = new GubaPost();
        post.setPostId(postId);
        post.setStockCode(stockCode);
        post.setUrl(String.format(GUBA_POST_URL, stockCode, postId));

        // 首先尝试从嵌入的 JSON 中提取数据
        int startIndex = html.indexOf("var post_article=");
        if (startIndex != -1) {
            try {
                int jsonStart = html.indexOf("{", startIndex);
                if (jsonStart != -1) {
                    // 使用括号匹配找到 JSON 结束位置
                    int braceCount = 0;
                    int endIndex = jsonStart;
                    boolean inString = false;
                    boolean escape = false;

                    for (int i = jsonStart; i < html.length(); i++) {
                        char c = html.charAt(i);

                        if (escape) {
                            escape = false;
                            continue;
                        }

                        if (c == '\\' && inString) {
                            escape = true;
                            continue;
                        }

                        if (c == '"') {
                            inString = !inString;
                            continue;
                        }

                        if (!inString) {
                            if (c == '{') {
                                braceCount++;
                            } else if (c == '}') {
                                braceCount--;
                                if (braceCount == 0) {
                                    endIndex = i + 1;
                                    break;
                                }
                            }
                        }
                    }

                    String jsonStr = html.substring(jsonStart, endIndex);
                    JsonNode node = objectMapper.readTree(jsonStr);

                    post.setTitle(textValue(node, "post_title"));
                    post.setContent(textValue(node, "post_content"));
                    post.setAuthor(textValue(node.path("post_user"), "user_nickname"));
                    post.setAuthorId(textValue(node.path("post_user"), "user_id"));
                    post.setReadCount(node.path("post_click_count").asInt(0));
                    post.setCommentCount(node.path("post_comment_count").asInt(0));
                    post.setLikeCount(node.path("post_like_count").asInt(0));

                    String postTimeStr = textValue(node, "post_publish_time");
                    if (postTimeStr != null) {
                        post.setPostTime(LocalDateTime.parse(postTimeStr, DATE_FORMATTER));
                    }

                    return post;
                }
            } catch (Exception e) {
                log.warn("Failed to parse embedded JSON, falling back to HTML: {}", e.getMessage());
            }
        }

        // 备用：从 HTML 中解析
        // 标题
        Element titleEl = doc.selectFirst("div.newstitle");
        if (titleEl != null) {
            post.setTitle(titleEl.text().trim());
        }

        // 作者
        Element authorEl = doc.selectFirst("a.name");
        if (authorEl != null) {
            post.setAuthor(authorEl.text().trim());
        }

        // 内容 - 使用 div.newstext
        Element contentEl = doc.selectFirst("div.newstext");
        if (contentEl != null) {
            post.setContent(contentEl.text().trim());
        }

        // 时间
        Element timeEl = doc.selectFirst("div.time");
        if (timeEl != null) {
            post.setPostTime(parseTime(timeEl.text().trim()));
        }

        return post;
    }

    /**
     * 获取帖子评论列表
     *
     * @param postId   帖子ID
     * @param pageSize 每页数量
     * @param page     页码
     * @return 评论列表
     */
    public List<GubaComment> getComments(String postId, int pageSize, int page) throws IOException {
        String url = String.format(GUBA_COMMENT_API, postId, pageSize, page);
        log.info("Fetching comments for post: {}", postId);

        Map<String, String> headers = Map.of(
                "Referer", "https://guba.eastmoney.com/"
        );

        String json = HttpUtils.getEastMoney(
                url, headers, CrawlerRequestPolicy.backgroundNews());
        return parseComments(json, postId);
    }

    /**
     * 获取帖子评论（默认 20 条）
     */
    public List<GubaComment> getComments(String postId) throws IOException {
        return getComments(postId, 20, 1);
    }

    /**
     * 解析评论列表
     */
    private List<GubaComment> parseComments(String response, String postId) throws IOException {
        List<GubaComment> comments = new ArrayList<>();

        // CDN API 返回格式: var reply_api_Reply_ArticleNewReplyList={...};//at ...
        int jsonStart = response.indexOf("=");
        int jsonEnd = response.indexOf(";//");
        if (jsonStart == -1 || jsonEnd == -1) {
            log.warn("Unexpected comment API response format for post: {}", postId);
            return comments;
        }
        String json = response.substring(jsonStart + 1, jsonEnd);

        JsonNode root = objectMapper.readTree(json);
        JsonNode listNode = root.path("re");
        if (listNode.isArray()) {
            for (JsonNode node : listNode) {
                GubaComment comment = new GubaComment();
                comment.setPostId(postId);
                comment.setCommentId(textValue(node, "reply_id"));
                comment.setContent(textValue(node, "reply_text"));
                comment.setLikeCount(node.path("reply_like_count").asInt(0));

                // 新版作者信息在 reply_user 对象中
                JsonNode userNode = node.path("reply_user");
                comment.setAuthor(textValue(userNode, "user_nickname"));
                comment.setAuthorId(textValue(userNode, "user_id"));

                String timeStr = textValue(node, "reply_publish_time");
                if (timeStr != null) {
                    comment.setPostTime(parseTime(timeStr));
                }

                comments.add(comment);
            }
        }

        log.info("Parsed {} comments", comments.size());
        return comments;
    }

    /**
     * 获取热度最高的前几个帖子的评论
     * 按阅读量 + 评论数 + 点赞数综合排序，取 top N 帖子后抓取其评论
     *
     * @param stockCode 股票代码 (如 "000001")
     * @param topN      取前几个帖子
     * @param commentPageSize 每个帖子取几条评论
     * @return 评论列表
     */
    public List<GubaComment> getTopPostComments(String stockCode, int topN, int commentPageSize) throws IOException {
        List<GubaPost> posts = getPostList(stockCode, 20, 1);

        // 按热度排序：阅读量权重最高，其次评论数、点赞数
        posts.sort((a, b) -> {
            long scoreA = (a.getReadCount() != null ? a.getReadCount() : 0) * 10L
                    + (a.getCommentCount() != null ? a.getCommentCount() : 0) * 5L
                    + (a.getLikeCount() != null ? a.getLikeCount() : 0);
            long scoreB = (b.getReadCount() != null ? b.getReadCount() : 0) * 10L
                    + (b.getCommentCount() != null ? b.getCommentCount() : 0) * 5L
                    + (b.getLikeCount() != null ? b.getLikeCount() : 0);
            return Long.compare(scoreB, scoreA);
        });

        List<GubaComment> allComments = new ArrayList<>();
        int count = Math.min(topN, posts.size());
        for (int i = 0; i < count; i++) {
            GubaPost post = posts.get(i);
            if (post.getPostId() == null) {
                continue;
            }
            try {
                List<GubaComment> comments = getComments(post.getPostId(), commentPageSize, 1);
                allComments.addAll(comments);
            } catch (IOException e) {
                log.warn("Failed to fetch comments for post {} ({}): {}",
                        post.getPostId(), post.getTitle(), e.getMessage());
            }
        }

        log.info("Got {} comments from top {} posts for stock {}", allComments.size(), count, stockCode);
        return allComments;
    }

    /**
     * 获取热度最高的前3个帖子的评论（每个帖子取20条）
     */
    public List<GubaComment> getTop3PostComments(String stockCode) throws IOException {
        return getTopPostComments(stockCode, 3, 20);
    }

    /**
     * 解析时间字符串
     */
    private LocalDateTime parseTime(String text) {
        if (text == null || text.isEmpty()) return null;

        try {
            // 格式: "2026-03-25 10:30:00"
            if (text.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
                return LocalDateTime.parse(text, DATE_FORMATTER);
            }
            // 格式: "03-25 10:30" (当年)
            if (text.matches("\\d{2}-\\d{2} \\d{2}:\\d{2}")) {
                int year = LocalDateTime.now().getYear();
                return LocalDateTime.parse(year + "-" + text, DATE_FORMATTER);
            }
            // 格式: "10:30" (今天)
            if (text.matches("\\d{2}:\\d{2}")) {
                return LocalDateTime.now().withHour(Integer.parseInt(text.substring(0, 2)))
                        .withMinute(Integer.parseInt(text.substring(3, 5)))
                        .withSecond(0);
            }
            // 格式: "昨天 10:30"
            if (text.startsWith("昨天")) {
                String time = text.replace("昨天", "").trim();
                LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
                if (time.matches("\\d{2}:\\d{2}")) {
                    return yesterday.withHour(Integer.parseInt(time.substring(0, 2)))
                            .withMinute(Integer.parseInt(time.substring(3, 5)));
                }
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
        return null;
    }

    private String textValue(JsonNode parent, String fieldName) {
        JsonNode value = parent == null ? null : parent.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }
}
