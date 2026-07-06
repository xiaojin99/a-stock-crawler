package com.stock.crawler.model;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 个股所属行业/概念/地域板块。
 */
public class ConceptBlock implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String code;
    private BigDecimal changePercent;
    private String leadStock;

    public ConceptBlock() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public BigDecimal getChangePercent() { return changePercent; }
    public void setChangePercent(BigDecimal changePercent) { this.changePercent = changePercent; }
    public String getLeadStock() { return leadStock; }
    public void setLeadStock(String leadStock) { this.leadStock = leadStock; }

    public static class Builder {
        private final ConceptBlock instance = new ConceptBlock();

        public Builder name(String name) { instance.name = name; return this; }
        public Builder code(String code) { instance.code = code; return this; }
        public Builder changePercent(BigDecimal changePercent) { instance.changePercent = changePercent; return this; }
        public Builder leadStock(String leadStock) { instance.leadStock = leadStock; return this; }
        public ConceptBlock build() { return instance; }
    }
}
