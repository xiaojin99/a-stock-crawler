package com.stock.web.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * Web 展示层外部链接安全检查。
 */
public final class SafeUrlUtils {

    private SafeUrlUtils() {
    }

    public static boolean isSafeHttpUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            URI uri = new URI(url.trim());
            String scheme = uri.getScheme();
            if (scheme == null || uri.getHost() == null) {
                return false;
            }
            String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
            return "http".equals(normalizedScheme) || "https".equals(normalizedScheme);
        } catch (URISyntaxException ex) {
            return false;
        }
    }
}
