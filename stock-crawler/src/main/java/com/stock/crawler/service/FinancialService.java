package com.stock.crawler.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.crawler.model.FinancialIndicator;
import com.stock.crawler.model.InstitutionalHolding;
import com.stock.crawler.model.ShareholderConcentration;
import com.stock.crawler.util.HttpUtils;
import com.stock.crawler.util.StockCodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 财务数据服务
 * 调用东方财富数据中心 API 获取个股主要财务指标和机构持仓数据
 *
 * <p>接口基础地址：https://datacenter.eastmoney.com/securities/api/data/v1/get</p>
 */
public class FinancialService {

    private static final Logger log = LoggerFactory.getLogger(FinancialService.class);

    private static final String DATACENTER_API =
            "https://datacenter.eastmoney.com/securities/api/data/v1/get";

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 主要财务指标 ====================

    /**
     * 获取个股主要财务指标（最近 N 个报告期）
     *
     * @param stockCode 股票代码（如 "000001"，支持带前缀如 "sz000001"）
     * @param count     报告期数量（默认 4）
     * @return 财务指标列表，按报告期降序
     */
    public List<FinancialIndicator> getMainFinancialIndicators(String stockCode, int count) throws IOException {
        String code = stripCodePrefix(stockCode);
        int safeCount = StockCodeUtils.clamp(count, 1, 20);
        String filter = URLEncoder.encode("(SECURITY_CODE=\"" + code + "\")", StandardCharsets.UTF_8);

        String url = String.format("%s?reportName=RPT_F10_FINANCE_MAINFINADATA"
                        + "&columns=ALL"
                        + "&filter=%s"
                        + "&pageNumber=1&pageSize=%d"
                        + "&sortTypes=-1&sortColumns=REPORT_DATE"
                        + "&source=HSF10&client=PC",
                DATACENTER_API, filter, safeCount);

        log.info("fetching_financial_indicators code={}", code);

        String json = HttpUtils.getEastMoney(url, Map.of("Referer", "https://data.eastmoney.com/"));
        return parseFinancialIndicators(json, code);
    }

    /**
     * 获取个股最近 4 个报告期的财务指标
     */
    public List<FinancialIndicator> getMainFinancialIndicators(String stockCode) throws IOException {
        return getMainFinancialIndicators(stockCode, 4);
    }

    // ==================== 股东集中度 ====================

    /**
     * 获取个股股东集中度数据（最近 N 个报告期）
     *
     * @param stockCode 股票代码
     * @param count     报告期数量（默认 4）
     * @return 股东集中度列表，按报告期降序
     */
    public List<ShareholderConcentration> getShareholderConcentration(String stockCode, int count) throws IOException {
        String code = stripCodePrefix(stockCode);
        int safeCount = StockCodeUtils.clamp(count, 1, 20);
        String filter = URLEncoder.encode("(SECURITY_CODE=\"" + code + "\")", StandardCharsets.UTF_8);

        String url = String.format("%s?reportName=RPT_F10_EH_HOLDERNUM"
                        + "&columns=ALL"
                        + "&filter=%s"
                        + "&pageNumber=1&pageSize=%d"
                        + "&sortTypes=-1&sortColumns=END_DATE"
                        + "&source=HSF10&client=PC",
                DATACENTER_API, filter, safeCount);

        log.info("fetching_shareholder_concentration code={}", code);

        String json = HttpUtils.getEastMoney(url, Map.of("Referer", "https://data.eastmoney.com/"));
        return parseShareholderConcentration(json, code);
    }

    /**
     * 获取个股最近 4 个报告期的股东集中度
     */
    public List<ShareholderConcentration> getShareholderConcentration(String stockCode) throws IOException {
        return getShareholderConcentration(stockCode, 4);
    }

    // ==================== 格式化 ====================

    /**
     * 将财务指标格式化为 Agent 可读文本
     */
    public String formatFinancialIndicators(List<FinancialIndicator> indicators) {
        if (indicators == null || indicators.isEmpty()) {
            return "暂无财务指标数据";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## 主要财务指标\n\n");
        sb.append("| 报告期 | 营收(亿) | 同比 | 净利润(亿) | 同比 | 毛利率 | 净利率 | ROE | EPS | 资产负债率 |\n");
        sb.append("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |\n");

        for (FinancialIndicator fi : indicators) {
            sb.append(String.format("| %s | %s | %s | %s | %s | %s | %s | %s | %s | %s |\n",
                    fi.getReportDateName() != null ? fi.getReportDateName() : "-",
                    formatAmount(fi.getTotalOperateIncome()),
                    formatPercent(fi.getTotalOperateIncomeYoy()),
                    formatAmount(fi.getParentNetprofit()),
                    formatPercent(fi.getParentNetprofitYoy()),
                    formatPercent(fi.getGrossProfitMargin()),
                    formatPercent(fi.getNetProfitMargin()),
                    formatPercent(fi.getRoeWeighted()),
                    fi.getBasicEps() != null ? String.format("%.2f", fi.getBasicEps()) : "-",
                    formatPercent(fi.getDebtAssetRatio())
            ));
        }

        return sb.toString();
    }

    /**
     * 将股东集中度格式化为 Agent 可读文本
     */
    public String formatShareholderConcentration(List<ShareholderConcentration> data) {
        if (data == null || data.isEmpty()) {
            return "暂无股东集中度数据";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## 股东集中度变动\n\n");
        sb.append("| 报告期 | 股东户数 | 较上期变化 | 户均持股(股) | 户均持股市值(万) | 筹码集中度 | 流通股占比 |\n");
        sb.append("| --- | --- | --- | --- | --- | --- | --- |\n");

        for (ShareholderConcentration sc : data) {
            String period = sc.getEndDate() != null ? sc.getEndDate().substring(0, 10) : "-";
            sb.append(String.format("| %s | %s | %s | %s | %s | %s | %s |\n",
                    period,
                    sc.getHolderTotalNum() != null ? String.format("%,d", sc.getHolderTotalNum()) : "-",
                    sc.getTotalNumRatio() != null ? String.format("%.2f%%", sc.getTotalNumRatio()) : "-",
                    sc.getAvgFreeShares() != null ? String.format("%.0f", sc.getAvgFreeShares()) : "-",
                    sc.getAvgHoldAmt() != null ? String.format("%.2f万", sc.getAvgHoldAmt() / 10000) : "-",
                    sc.getHoldFocus() != null ? sc.getHoldFocus() : "-",
                    sc.getHoldRatioTotal() != null ? String.format("%.2f%%", sc.getHoldRatioTotal()) : "-"
            ));
        }

        return sb.toString();
    }

    // ==================== 解析 ====================

    private List<FinancialIndicator> parseFinancialIndicators(String json, String stockCode) throws IOException {
        JsonNode root = objectMapper.readTree(json);
        JsonNode resultNode = root.path("result");
        JsonNode dataNode = resultNode.path("data");

        List<FinancialIndicator> list = new ArrayList<>();
        if (!dataNode.isArray()) {
            log.warn("financial_indicator_data_missing code={}", stockCode);
            return list;
        }

        for (JsonNode node : dataNode) {
            FinancialIndicator fi = new FinancialIndicator();
            fi.setSecurityCode(textValue(node, "SECURITY_CODE"));
            fi.setSecurityNameAbbr(textValue(node, "SECURITY_NAME_ABBR"));
            fi.setReportDate(textValue(node, "REPORT_DATE"));
            fi.setReportDateName(textValue(node, "REPORT_DATE_NAME"));
            fi.setTotalOperateIncome(doubleValue(node, "TOTALOPERATEREVE"));
            fi.setTotalOperateIncomeYoy(doubleValue(node, "TOTALOPERATEREVETZ"));
            fi.setParentNetprofit(doubleValue(node, "PARENTNETPROFIT"));
            fi.setParentNetprofitYoy(doubleValue(node, "PARENTNETPROFITTZ"));
            fi.setCutParentNetprofit(doubleValue(node, "KCFJCXSYJLR"));
            fi.setCutParentNetprofitYoy(doubleValue(node, "KCFJCXSYJLRTZ"));
            fi.setGrossProfitMargin(doubleValue(node, "XSMLL"));
            fi.setNetProfitMargin(doubleValue(node, "XSJLL"));
            fi.setRoeWeighted(doubleValue(node, "ROEJQ"));
            fi.setBasicEps(doubleValue(node, "EPSJB"));
            fi.setBps(doubleValue(node, "BPS"));
            fi.setDebtAssetRatio(doubleValue(node, "ZCFZL"));
            fi.setOperateCashFlowPs(doubleValue(node, "MGJYXJJE"));
            list.add(fi);
        }

        log.info("financial_indicators_fetched code={} count={}", stockCode, list.size());
        return list;
    }

    private List<ShareholderConcentration> parseShareholderConcentration(String json, String stockCode) throws IOException {
        JsonNode root = objectMapper.readTree(json);
        JsonNode resultNode = root.path("result");
        JsonNode dataNode = resultNode.path("data");

        List<ShareholderConcentration> list = new ArrayList<>();
        if (!dataNode.isArray()) {
            log.warn("shareholder_concentration_data_missing code={}", stockCode);
            return list;
        }

        for (JsonNode node : dataNode) {
            ShareholderConcentration sc = new ShareholderConcentration();
            sc.setSecurityCode(textValue(node, "SECURITY_CODE"));
            sc.setSecurityNameAbbr(textValue(node, "SECURITY_NAME_ABBR"));
            sc.setEndDate(textValue(node, "END_DATE"));
            sc.setNoticeDate(textValue(node, "NOTICE_DATE"));
            sc.setHolderTotalNum(intValue(node, "HOLDER_TOTAL_NUM"));
            sc.setTotalNumRatio(doubleValue(node, "TOTAL_NUM_RATIO"));
            sc.setAvgFreeShares(doubleValue(node, "AVG_FREE_SHARES"));
            sc.setAvgHoldAmt(doubleValue(node, "AVG_HOLD_AMT"));
            sc.setHoldFocus(textValue(node, "HOLD_FOCUS"));
            sc.setHoldRatioTotal(doubleValue(node, "HOLD_RATIO_TOTAL"));
            sc.setHolderTotalNumChange(intValue(node, "HOLDER_TOTAL_NUMCHANGE"));
            list.add(sc);
        }

        log.info("shareholder_concentration_fetched code={} count={}", stockCode, list.size());
        return list;
    }

    // ==================== 工具方法 ====================

    private String stripCodePrefix(String stockCode) {
        return StockCodeUtils.stripMarket(stockCode);
    }

    private Double doubleValue(JsonNode node, String field) {
        JsonNode n = node.get(field);
        if (n == null || n.isNull() || !n.isNumber()) return null;
        return n.doubleValue();
    }

    private Integer intValue(JsonNode node, String field) {
        JsonNode n = node.get(field);
        if (n == null || n.isNull() || !n.isNumber()) return null;
        return n.intValue();
    }

    private String textValue(JsonNode node, String field) {
        JsonNode n = node.get(field);
        if (n == null || n.isNull()) {
            return null;
        }
        return n.asText();
    }

    private String formatAmount(Double amount) {
        if (amount == null) return "-";
        double yi = amount / 100_000_000;
        if (Math.abs(yi) >= 1) {
            return String.format("%.2f亿", yi);
        }
        double wan = amount / 10_000;
        if (Math.abs(wan) >= 1) {
            return String.format("%.2f万", wan);
        }
        return String.format("%.2f", amount);
    }

    private String formatPercent(Double percent) {
        if (percent == null) return "-";
        return String.format("%.2f%%", percent);
    }
}
