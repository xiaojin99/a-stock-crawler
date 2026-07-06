package com.stock.crawler.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 热点条目数据结构
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HotItem {

    private String id;
    private String title;
    private String url;
    private Integer hotScore;
    private Integer rank;
    private String platform;
    private String extra;

    public HotItem() {
    }

    public HotItem(String id, String title, String url, Integer rank, String platform) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.rank = rank;
        this.platform = platform;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getHotScore() {
        return hotScore;
    }

    public void setHotScore(Integer hotScore) {
        this.hotScore = hotScore;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    @Override
    public String toString() {
        return "HotItem{" +
                "rank=" + rank +
                ", title='" + title + '\'' +
                ", hotScore=" + hotScore +
                ", platform='" + platform + '\'' +
                '}';
    }
}
