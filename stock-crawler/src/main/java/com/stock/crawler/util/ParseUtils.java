package com.stock.crawler.util;

import java.math.BigDecimal;

/**
 * 数值解析工具：将字符串安全解析为 BigDecimal/long/double，解析失败或为空返回 0。
 * 供各行情数据源解析可能为空、为 "--" 或非数字的字段时统一使用。
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
