package com.stock.web.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafeUrlUtilsTest {

    @Test
    void allowsOnlyHttpAndHttpsUrls() {
        assertTrue(SafeUrlUtils.isSafeHttpUrl("https://finance.example.com/news"));
        assertTrue(SafeUrlUtils.isSafeHttpUrl("http://finance.example.com/news"));
        assertFalse(SafeUrlUtils.isSafeHttpUrl("javascript:alert(1)"));
        assertFalse(SafeUrlUtils.isSafeHttpUrl("data:text/html,<script>alert(1)</script>"));
        assertFalse(SafeUrlUtils.isSafeHttpUrl("//evil.example.com/no-scheme"));
        assertFalse(SafeUrlUtils.isSafeHttpUrl("not a url"));
    }
}
