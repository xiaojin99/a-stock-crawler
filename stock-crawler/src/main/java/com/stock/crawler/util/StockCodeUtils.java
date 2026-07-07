package com.stock.crawler.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 股票代码规范化与边界参数工具。
 */
public final class StockCodeUtils {

    private static final Pattern PREFIX_CODE = Pattern.compile("^(sh|sz|bj)(\\d{6})$");
    private static final Pattern SUFFIX_CODE = Pattern.compile("^(\\d{6})\\.(sh|sz|bj)$");
    private static final Pattern PURE_CODE = Pattern.compile("^\\d{6}$");

    private StockCodeUtils() {
    }

    public static String normalizeWithMarket(String stockCode) {
        ParsedStockCode parsed = parse(stockCode);
        return parsed.market + parsed.code;
    }

    public static String stripMarket(String stockCode) {
        return parse(stockCode).code;
    }

    public static boolean isValidAStockCode(String stockCode) {
        try {
            parse(stockCode);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static int clamp(int value, int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("min must be <= max");
        }
        return Math.max(min, Math.min(value, max));
    }

    private static ParsedStockCode parse(String stockCode) {
        if (stockCode == null) {
            throw new IllegalArgumentException("Stock code must not be null");
        }
        String code = stockCode.trim().toLowerCase(Locale.ROOT);
        if (code.isBlank()) {
            throw new IllegalArgumentException("Stock code must not be blank");
        }

        Matcher prefixMatcher = PREFIX_CODE.matcher(code);
        if (prefixMatcher.matches()) {
            return new ParsedStockCode(prefixMatcher.group(1), prefixMatcher.group(2));
        }

        Matcher suffixMatcher = SUFFIX_CODE.matcher(code);
        if (suffixMatcher.matches()) {
            return new ParsedStockCode(suffixMatcher.group(2), suffixMatcher.group(1));
        }

        if (PURE_CODE.matcher(code).matches()) {
            return new ParsedStockCode(inferMarket(code), code);
        }

        throw new IllegalArgumentException("Invalid stock code: " + stockCode);
    }

    private static String inferMarket(String code) {
        if (code.startsWith("6")) {
            return "sh";
        }
        if (code.startsWith("0") || code.startsWith("2") || code.startsWith("3")) {
            return "sz";
        }
        if (code.startsWith("4") || code.startsWith("8")) {
            return "bj";
        }
        throw new IllegalArgumentException("Cannot infer stock market for code: " + code);
    }

    private record ParsedStockCode(String market, String code) {
    }
}
