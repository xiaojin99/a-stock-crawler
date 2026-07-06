package com.stock.crawler.model;

import java.io.Serializable;

/**
 * 热榜条目
 */
public class MarketHotItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer rank;
    private String title;
    private String subtitle;
    private String time;
    private String heat;
    private String url;

    public MarketHotItem() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public Integer getRank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getHeat() { return heat; }
    public void setHeat(String heat) { this.heat = heat; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public static class Builder {
        private final MarketHotItem instance = new MarketHotItem();

        public Builder rank(Integer rank) { instance.rank = rank; return this; }
        public Builder title(String title) { instance.title = title; return this; }
        public Builder subtitle(String subtitle) { instance.subtitle = subtitle; return this; }
        public Builder time(String time) { instance.time = time; return this; }
        public Builder heat(String heat) { instance.heat = heat; return this; }
        public Builder url(String url) { instance.url = url; return this; }
        public MarketHotItem build() { return instance; }
    }

    @Override
    public String toString() {
        return "MarketHotItem{rank=" + rank + ", title='" + title + "', heat='" + heat + "'}";
    }
}
