package com.stock.crawler.kline;

import com.stock.crawler.model.KLineData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * 将日 K 线确定性聚合为周、月、年 K 线。
 */
public final class KLineAggregator {

    private static final int MAX_DAILY_BARS = 13_000;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final WeekFields ISO_WEEK = WeekFields.ISO;

    private KLineAggregator() {
    }

    /**
     * 聚合日 K 数据，并在完成涨跌幅计算后截取最近 {@code bars} 根。
     */
    public static List<KLineData> aggregate(List<KLineData> dailyKlines, String period, int bars) {
        if (dailyKlines == null || dailyKlines.isEmpty() || bars <= 0) {
            return List.of();
        }

        String normalizedPeriod = normalizePeriod(period);
        List<KLineData> sorted = dailyKlines.stream()
                .filter(KLineAggregator::hasRequiredFields)
                .sorted(Comparator.comparing(KLineData::getDate))
                .toList();
        if (sorted.isEmpty()) {
            return List.of();
        }
        if ("day".equals(normalizedPeriod)) {
            return tail(sorted.stream().map(KLineAggregator::copyDaily).toList(), bars);
        }

        Map<Object, List<KLineData>> buckets = new LinkedHashMap<>();
        for (KLineData daily : sorted) {
            buckets.computeIfAbsent(bucketKey(daily, normalizedPeriod), ignored -> new ArrayList<>())
                    .add(daily);
        }

        List<KLineData> aggregated = buckets.values().stream()
                .map(bucket -> aggregateBucket(bucket, normalizedPeriod))
                .toList();
        applyChangePercent(aggregated);
        return tail(aggregated, bars);
    }

    /**
     * 计算所需日 K 根数，并保留一个前置周期用于计算首根涨跌幅。
     */
    public static int requiredDailyBars(String period, int bars) {
        if (bars <= 0) {
            return 0;
        }
        long required = switch (normalizePeriod(period)) {
            case "week" -> (long) (bars + 1) * 6;
            case "month" -> (long) (bars + 1) * 23;
            case "year" -> (long) (bars + 1) * 250;
            default -> bars;
        };
        return (int) Math.min(required, MAX_DAILY_BARS);
    }

    private static boolean hasRequiredFields(KLineData kline) {
        if (kline == null
                || kline.getDate() == null
                || !isPositive(kline.getOpen())
                || !isPositive(kline.getHigh())
                || !isPositive(kline.getLow())
                || !isPositive(kline.getClose())) {
            return false;
        }
        if (kline.getHigh().compareTo(kline.getLow()) < 0
                || kline.getHigh().compareTo(kline.getOpen()) < 0
                || kline.getHigh().compareTo(kline.getClose()) < 0
                || kline.getLow().compareTo(kline.getOpen()) > 0
                || kline.getLow().compareTo(kline.getClose()) > 0) {
            return false;
        }
        return (kline.getVolume() == null || kline.getVolume() >= 0)
                && (kline.getAmount() == null || kline.getAmount().signum() >= 0);
    }

    private static boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private static Object bucketKey(KLineData kline, String period) {
        return switch (period) {
            case "week" -> new WeekKey(
                    kline.getDate().get(ISO_WEEK.weekBasedYear()),
                    kline.getDate().get(ISO_WEEK.weekOfWeekBasedYear()));
            case "month" -> YearMonth.from(kline.getDate());
            case "year" -> kline.getDate().getYear();
            default -> kline.getDate();
        };
    }

    private static KLineData aggregateBucket(List<KLineData> bucket, String period) {
        KLineData first = bucket.getFirst();
        KLineData last = bucket.getLast();
        BigDecimal high = bucket.stream()
                .map(KLineData::getHigh)
                .max(BigDecimal::compareTo)
                .orElse(first.getHigh());
        BigDecimal low = bucket.stream()
                .map(KLineData::getLow)
                .min(BigDecimal::compareTo)
                .orElse(first.getLow());

        return KLineData.builder()
                .code(first.getCode())
                .date(last.getDate())
                .open(first.getOpen())
                .high(high)
                .low(low)
                .close(last.getClose())
                .volume(bucket.stream().mapToLong(item -> valueOrZero(item.getVolume())).sum())
                .amount(sumDecimal(bucket, KLineData::getAmount))
                .turnoverRate(sumDecimal(bucket, KLineData::getTurnoverRate))
                .period(period)
                .build();
    }

    private static BigDecimal sumDecimal(
            List<KLineData> bucket,
            Function<KLineData, BigDecimal> extractor) {
        return bucket.stream()
                .map(extractor)
                .map(KLineAggregator::valueOrZero)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static void applyChangePercent(List<KLineData> aggregated) {
        for (int index = 1; index < aggregated.size(); index++) {
            BigDecimal previousClose = aggregated.get(index - 1).getClose();
            if (previousClose == null || previousClose.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            BigDecimal changePercent = aggregated.get(index).getClose()
                    .subtract(previousClose)
                    .multiply(ONE_HUNDRED)
                    .divide(previousClose, 2, RoundingMode.HALF_UP);
            aggregated.get(index).setChangePercent(changePercent);
        }
    }

    private static KLineData copyDaily(KLineData daily) {
        return KLineData.builder()
                .code(daily.getCode())
                .date(daily.getDate())
                .open(daily.getOpen())
                .high(daily.getHigh())
                .low(daily.getLow())
                .close(daily.getClose())
                .volume(daily.getVolume())
                .amount(daily.getAmount())
                .changePercent(daily.getChangePercent())
                .turnoverRate(daily.getTurnoverRate())
                .period("day")
                .build();
    }

    private static <T> List<T> tail(List<T> values, int limit) {
        int fromIndex = Math.max(0, values.size() - limit);
        return new ArrayList<>(values.subList(fromIndex, values.size()));
    }

    private static long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private static BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String normalizePeriod(String period) {
        if (period == null) {
            return "day";
        }
        return switch (period.trim().toLowerCase(Locale.ROOT)) {
            case "week", "weekly", "w" -> "week";
            case "month", "monthly", "m" -> "month";
            case "year", "yearly", "y" -> "year";
            default -> "day";
        };
    }

    private record WeekKey(int weekBasedYear, int weekOfYear) {
    }
}
