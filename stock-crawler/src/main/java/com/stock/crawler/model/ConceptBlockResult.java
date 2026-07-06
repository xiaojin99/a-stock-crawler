package com.stock.crawler.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 个股板块归属结果。
 */
public class ConceptBlockResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private int total;
    private List<ConceptBlock> boards = new ArrayList<>();
    private List<String> conceptTags = new ArrayList<>();

    public ConceptBlockResult() {
    }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public List<ConceptBlock> getBoards() { return boards; }
    public void setBoards(List<ConceptBlock> boards) { this.boards = boards; }
    public List<String> getConceptTags() { return conceptTags; }
    public void setConceptTags(List<String> conceptTags) { this.conceptTags = conceptTags; }
}
