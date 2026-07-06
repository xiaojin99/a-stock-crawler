package com.stock.crawler.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 热榜分组
 */
public class MarketHotBoard implements Serializable {

    private static final long serialVersionUID = 1L;

    private String boardId;
    private String boardName;
    private String source;
    private String updateTime;
    private List<MarketHotItem> items = new ArrayList<>();

    public MarketHotBoard() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getBoardId() { return boardId; }
    public void setBoardId(String boardId) { this.boardId = boardId; }
    public String getBoardName() { return boardName; }
    public void setBoardName(String boardName) { this.boardName = boardName; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
    public List<MarketHotItem> getItems() { return items; }
    public void setItems(List<MarketHotItem> items) { this.items = items; }

    public static class Builder {
        private final MarketHotBoard instance = new MarketHotBoard();

        public Builder boardId(String boardId) { instance.boardId = boardId; return this; }
        public Builder boardName(String boardName) { instance.boardName = boardName; return this; }
        public Builder source(String source) { instance.source = source; return this; }
        public Builder updateTime(String updateTime) { instance.updateTime = updateTime; return this; }
        public Builder items(List<MarketHotItem> items) { instance.items = items; return this; }
        public MarketHotBoard build() { return instance; }
    }

    @Override
    public String toString() {
        return "MarketHotBoard{boardId='" + boardId + "', boardName='" + boardName + "', items=" + (items == null ? 0 : items.size()) + "}";
    }
}
