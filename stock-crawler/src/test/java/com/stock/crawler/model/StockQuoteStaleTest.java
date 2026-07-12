package com.stock.crawler.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StockQuoteStaleTest {

    @Test
    void stale_defaultsToFalseAndIsSupportedByBuilderAndSetter() {
        StockQuote fresh = new StockQuote();
        StockQuote stale = StockQuote.builder().stale(true).build();

        assertFalse(fresh.isStale());
        assertTrue(stale.isStale());

        fresh.setStale(true);
        assertTrue(fresh.isStale());
    }
}
