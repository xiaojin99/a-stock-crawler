package com.stock.crawler.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * 股吧帖子数据结构
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GubaPost {

    private String postId;          // 帖子ID
    private String stockCode;       // 股票代码
    private String stockName;       // 股票名称
    private String title;           // 帖子标题
    private String content;         // 帖子内容
    private String author;          // 作者
    private String authorId;        // 作者ID
    private LocalDateTime postTime; // 发帖时间
    private Integer readCount;      // 阅读量
    private Integer commentCount;   // 评论数
    private Integer likeCount;      // 点赞数
    private String url;             // 帖子链接
    private Boolean isTop;          // 是否置顶
    private Boolean isEssence;      // 是否精华

    public GubaPost() {
    }

    // Getters and Setters
    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getStockCode() {
        return stockCode;
    }

    public void setStockCode(String stockCode) {
        this.stockCode = stockCode;
    }

    public String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public LocalDateTime getPostTime() {
        return postTime;
    }

    public void setPostTime(LocalDateTime postTime) {
        this.postTime = postTime;
    }

    public Integer getReadCount() {
        return readCount;
    }

    public void setReadCount(Integer readCount) {
        this.readCount = readCount;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Boolean getIsTop() {
        return isTop;
    }

    public void setIsTop(Boolean isTop) {
        this.isTop = isTop;
    }

    public Boolean getIsEssence() {
        return isEssence;
    }

    public void setIsEssence(Boolean isEssence) {
        this.isEssence = isEssence;
    }

    @Override
    public String toString() {
        return "GubaPost{" +
                "postId='" + postId + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", readCount=" + readCount +
                ", commentCount=" + commentCount +
                '}';
    }
}
