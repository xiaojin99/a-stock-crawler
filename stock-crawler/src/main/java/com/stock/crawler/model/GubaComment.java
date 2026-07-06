package com.stock.crawler.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * 股吧评论数据结构
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GubaComment {

    private String commentId;       // 评论ID
    private String postId;          // 帖子ID
    private String content;         // 评论内容
    private String author;          // 评论者
    private String authorId;        // 评论者ID
    private LocalDateTime postTime; // 评论时间
    private Integer likeCount;      // 点赞数
    private Integer replyCount;     // 回复数
    private String replyTo;         // 回复对象（楼中楼）

    public GubaComment() {
    }

    // Getters and Setters
    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
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

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Integer getReplyCount() {
        return replyCount;
    }

    public void setReplyCount(Integer replyCount) {
        this.replyCount = replyCount;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    @Override
    public String toString() {
        return "GubaComment{" +
                "author='" + author + '\'' +
                ", content='" + (content != null && content.length() > 30 ? content.substring(0, 30) + "..." : content) + '\'' +
                ", likeCount=" + likeCount +
                '}';
    }
}
