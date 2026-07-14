package com.stock.crawler.util;

import java.math.BigDecimal;

/**
 * 数值解析工具。
 *
 * <p>兼容方法 {@code parseBigDecimal}/{@code parseLong}/{@code parseDouble}
 * 在解析失败时返回 0；数据源应按字段契约优先使用 required/nullable 方法，
 * 避免把必填字段损坏伪装成有效的零值。</p>
 */
public final class ParseUtils {

    private ParseUtils() {
    }

    public static BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 解析必填的十进制数值。
     *
     * @throws NumberFormatException 字段为空或不是合法十进制数时
     */
    public static BigDecimal parseRequiredBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            throw new NumberFormatException("Required decimal value is blank");
        }
        return new BigDecimal(value.trim());
    }

    /**
     * 解析可空的十进制数值；空值、占位符和非法值均表示字段不可用。
     */
    public static BigDecimal parseNullableBigDecimal(String value) {
        if (value == null || value.isBlank() || "--".equals(value.trim())) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static long parseLong(String value) {
        if (value == null || value.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    /**
     * 解析必填的长整型数值。
     *
     * @throws NumberFormatException 字段为空或不是合法长整型时
     */
    public static long parseRequiredLong(String value) {
        if (value == null || value.isBlank()) {
            throw new NumberFormatException("Required long value is blank");
        }
        return Long.parseLong(value.trim());
    }

    /**
     * 解析可空的长整型数值；空值、占位符和非法值均表示字段不可用。
     */
    public static Long parseNullableLong(String value) {
        if (value == null || value.isBlank() || "--".equals(value.trim())) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static double parseDouble(String value) {
        if (value == null || value.isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }
}
