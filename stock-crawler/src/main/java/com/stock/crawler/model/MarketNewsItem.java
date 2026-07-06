package com.stock.crawler.model;

import java.io.Serializable;

/**
 * 市场快讯条目
 */
public class MarketNewsItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private String title;
    private String content;
    private String time;
    private String url;
    private String source;

    public MarketNewsItem() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public static class Builder {
        private final MarketNewsItem instance = new MarketNewsItem();

        public Builder title(String title) { instance.title = title; return this; }
        public Builder content(String content) { instance.content = content; return this; }
        public Builder time(String time) { instance.time = time; return this; }
        public Builder url(String url) { instance.url = url; return this; }
        public Builder source(String source) { instance.source = source; return this; }
        public MarketNewsItem build() { return instance; }
    }

    @Override
    public String toString() {
        return "MarketNewsItem{title='" + title + "', source='" + source + "', time='" + time + "'}";
    }
}
