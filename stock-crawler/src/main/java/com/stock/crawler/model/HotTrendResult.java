package com.stock.crawler.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 热点结果数据结构
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HotTrendResult {

    private String platform;
    private String platformCN;
    private List<HotItem> items;
    private LocalDateTime updatedAt;
    private boolean fromCache;
    private String error;

    public HotTrendResult() {
    }

    public HotTrendResult(String platform, String platformCN, List<HotItem> items) {
        this.platform = platform;
        this.platformCN = platformCN;
        this.items = items;
        this.updatedAt = LocalDateTime.now();
    }

    public static HotTrendResult error(String platform, String platformCN, String error) {
        HotTrendResult result = new HotTrendResult();
        result.setPlatform(platform);
        result.setPlatformCN(platformCN);
        result.setError(error);
        return result;
    }

    // Getters and Setters
    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getPlatformCN() {
        return platformCN;
    }

    public void setPlatformCN(String platformCN) {
        this.platformCN = platformCN;
    }

    public List<HotItem> getItems() {
        return items;
    }

    public void setItems(List<HotItem> items) {
        this.items = items;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isFromCache() {
        return fromCache;
    }

    public void setFromCache(boolean fromCache) {
        this.fromCache = fromCache;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public boolean hasError() {
        return error != null && !error.isEmpty();
    }

    @Override
    public String toString() {
        return "HotTrendResult{" +
                "platform='" + platform + '\'' +
                ", platformCN='" + platformCN + '\'' +
                ", itemCount=" + (items != null ? items.size() : 0) +
                ", error='" + error + '\'' +
                '}';
    }
}
