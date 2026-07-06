package com.stock.crawler.model;

import java.io.Serializable;

/**
 * 上市公司公告。
 */
public class AnnouncementItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private String title;
    private String type;
    private String date;
    private String url;

    public AnnouncementItem() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public static class Builder {
        private final AnnouncementItem instance = new AnnouncementItem();

        public Builder title(String title) { instance.title = title; return this; }
        public Builder type(String type) { instance.type = type; return this; }
        public Builder date(String date) { instance.date = date; return this; }
        public Builder url(String url) { instance.url = url; return this; }
        public AnnouncementItem build() { return instance; }
    }
}
