package com.stock.crawler.service;

import com.stock.crawler.model.KLineData;
import com.stock.crawler.model.TechnicalIndicators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.DecimalNum;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * 技术指标计算服务
 * 基于 ta4j 库实现
 */
public class TechnicalIndicatorService {

    private static final Logger log = LoggerFactory.getLogger(TechnicalIndicatorService.class);

    /**
     * 计算技术指标
     */
    public TechnicalIndicators calculate(List<KLineData> klineData) {
        if (klineData == null || klineData.isEmpty()) {
            return null;
        }
        try {
            BarSeries series = buildBarSeries(klineData);
            int lastIndex = series.getEndIndex();
            KLineData latestKline = klineData.get(klineData.size() - 1);

            ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
            VolumeIndicator volume = new VolumeIndicator(series);

            // 均线
            SMAIndicator sma5 = new SMAIndicator(closePrice, 5);
            SMAIndicator sma10 = new SMAIndicator(closePrice, 10);
            SMAIndicator sma20 = new SMAIndicator(closePrice, 20);
            SMAIndicator sma60 = new SMAIndicator(closePrice, 60);

            // MACD
            MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
            EMAIndicator macdSignal = new EMAIndicator(macd, 9);

            // RSI
            RSIIndicator rsi6 = new RSIIndicator(closePrice, 6);
            RSIIndicator rsi12 = new RSIIndicator(closePrice, 12);
            RSIIndicator rsi24 = new RSIIndicator(closePrice, 24);

            // 布林带
            SMAIndicator bollSma = new SMAIndicator(closePrice, 20);
            StandardDeviationIndicator sd = new StandardDeviationIndicator(closePrice, 20);
            BollingerBandsMiddleIndicator bollMiddle = new BollingerBandsMiddleIndicator(bollSma);
            BollingerBandsUpperIndicator bollUpper = new BollingerBandsUpperIndicator(bollMiddle, sd, DecimalNum.valueOf(2));
            BollingerBandsLowerIndicator bollLower = new BollingerBandsLowerIndicator(bollMiddle, sd, DecimalNum.valueOf(2));

            // 成交量均线
            SMAIndicator volumeSma5 = new SMAIndicator(volume, 5);
            SMAIndicator volumeSma10 = new SMAIndicator(volume, 10);

            TechnicalIndicators indicators = TechnicalIndicators.builder()
                    .code(latestKline.getCode())
                    .date(latestKline.getDate())
                    .ma5(toBigDecimal(sma5.getValue(lastIndex)))
                    .ma10(toBigDecimal(sma10.getValue(lastIndex)))
                    .ma20(toBigDecimal(sma20.getValue(lastIndex)))
                    .ma60(getValueOrNull(sma60, lastIndex))
                    .macdDif(toBigDecimal(macd.getValue(lastIndex)))
                    .macdDea(toBigDecimal(macdSignal.getValue(lastIndex)))
                    .macdHistogram(toBigDecimal(macd.getValue(lastIndex).minus(macdSignal.getValue(lastIndex))))
                    .rsi6(toBigDecimal(rsi6.getValue(lastIndex)))
                    .rsi12(toBigDecimal(rsi12.getValue(lastIndex)))
                    .rsi24(toBigDecimal(rsi24.getValue(lastIndex)))
                    .bollUpper(toBigDecimal(bollUpper.getValue(lastIndex)))
                    .bollMiddle(toBigDecimal(bollMiddle.getValue(lastIndex)))
                    .bollLower(toBigDecimal(bollLower.getValue(lastIndex)))
                    .volumeMa5(toBigDecimal(volumeSma5.getValue(lastIndex)))
                    .volumeMa10(toBigDecimal(volumeSma10.getValue(lastIndex)))
                    .build();

            indicators.setTrend(determineTrend(indicators, latestKline.getClose()));
            indicators.setMacdSignal(determineMacdSignal(indicators));
            indicators.setRsiSignal(determineRsiSignal(indicators));

            return indicators;
        } catch (RuntimeException ex) {
            log.error("technical_indicators_calculate_failed klineSize={} message={}",
                    klineData.size(), ex.getMessage(), ex);
            return null;
        }
    }

    private BarSeries buildBarSeries(List<KLineData> klineData) {
        BaseBarSeries series = new BaseBarSeries();
        for (KLineData kline : klineData) {
            ZonedDateTime time = kline.getDate().atStartOfDay(ZoneId.systemDefault());
            Bar bar = BaseBar.builder()
                    .timePeriod(Duration.ofDays(1))
                    .endTime(time)
                    .openPrice(DecimalNum.valueOf(kline.getOpen()))
                    .highPrice(DecimalNum.valueOf(kline.getHigh()))
                    .lowPrice(DecimalNum.valueOf(kline.getLow()))
                    .closePrice(DecimalNum.valueOf(kline.getClose()))
                    .volume(DecimalNum.valueOf(kline.getVolume()))
                    .build();
            series.addBar(bar);
        }
        return series;
    }

    private String determineTrend(TechnicalIndicators indicators, BigDecimal closePrice) {
        if (indicators.getMa5() == null || indicators.getMa10() == null || indicators.getMa20() == null) {
            return "UNKNOWN";
        }
        boolean aboveMa5 = closePrice.compareTo(indicators.getMa5()) > 0;
        boolean aboveMa10 = closePrice.compareTo(indicators.getMa10()) > 0;
        boolean aboveMa20 = closePrice.compareTo(indicators.getMa20()) > 0;
        boolean ma5AboveMa10 = indicators.getMa5().compareTo(indicators.getMa10()) > 0;
        boolean ma10AboveMa20 = indicators.getMa10().compareTo(indicators.getMa20()) > 0;

        if (aboveMa5 && aboveMa10 && aboveMa20 && ma5AboveMa10 && ma10AboveMa20) {
            return "UP";
        } else if (!aboveMa5 && !aboveMa10 && !aboveMa20 && !ma5AboveMa10 && !ma10AboveMa20) {
            return "DOWN";
        } else {
            return "SIDEWAYS";
        }
    }

    private String determineMacdSignal(TechnicalIndicators indicators) {
        if (indicators.getMacdDif() == null || indicators.getMacdDea() == null) {
            return "NEUTRAL";
        }
        boolean difAboveZero = indicators.getMacdDif().compareTo(BigDecimal.ZERO) > 0;
        boolean deaAboveZero = indicators.getMacdDea().compareTo(BigDecimal.ZERO) > 0;
        boolean difAboveDea = indicators.getMacdDif().compareTo(indicators.getMacdDea()) > 0;
        boolean histogramPositive = indicators.getMacdHistogram().compareTo(BigDecimal.ZERO) > 0;

        if (histogramPositive && !difAboveZero && difAboveDea) {
            return "GOLDEN_CROSS";
        } else if (!histogramPositive && difAboveZero && !difAboveDea) {
            return "DEATH_CROSS";
        } else if (difAboveZero && deaAboveZero && histogramPositive) {
            return "UP";
        } else if (!difAboveZero && !deaAboveZero && !histogramPositive) {
            return "DOWN";
        } else {
            return "NEUTRAL";
        }
    }

    private String determineRsiSignal(TechnicalIndicators indicators) {
        if (indicators.getRsi6() == null) {
            return "NEUTRAL";
        }
        BigDecimal rsi = indicators.getRsi6();
        if (rsi.compareTo(new BigDecimal("80")) > 0) {
            return "OVERBOUGHT";
        } else if (rsi.compareTo(new BigDecimal("20")) < 0) {
            return "OVERSOLD";
        } else {
            return "NEUTRAL";
        }
    }

    private BigDecimal toBigDecimal(org.ta4j.core.num.Num num) {
        return new BigDecimal(num.toString()).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal getValueOrNull(SMAIndicator indicator, int index) {
        try {
            return toBigDecimal(indicator.getValue(index));
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
