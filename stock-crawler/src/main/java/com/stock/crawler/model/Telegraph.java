package com.stock.crawler.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 财经快讯数据结构
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Telegraph {

    private String time;
    private String content;
    private String url;

    public Telegraph() {
    }

    public Telegraph(String time, String content, String url) {
        this.time = time;
        this.content = content;
        this.url = url;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "Telegraph{" +
                "time='" + time + '\'' +
                ", content='" + content + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
