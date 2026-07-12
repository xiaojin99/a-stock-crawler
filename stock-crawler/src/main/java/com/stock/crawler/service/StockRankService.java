package com.stock.crawler.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.crawler.model.StockRankItem;
import com.stock.crawler.util.CrawlerRequestPolicy;
import com.stock.crawler.util.HttpUtils;
import com.stock.crawler.util.ParseUtils;
import com.stock.crawler.util.StockCodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 个股人气榜服务
 * 获取东方财富个股人气排行榜数据
 */
public class StockRankService {

    private static final Logger log = LoggerFactory.getLogger(StockRankService.class);

    // 人气榜 API
    private static final String RANK_LIST_API = "https://emappdata.eastmoney.com/stockrank/getAllCurrentList";
    // 股票信息查询 API (批量获取股票名称和价格)
    private static final String STOCK_INFO_API = "https://push2.eastmoney.com/api/qt/ulist.np/get";
    // 腾讯财经备用接口
    private static final String TENCENT_QUOTE_URL = "https://qt.gtimg.cn/q=%s";
    private static final int TENCENT_BATCH_SIZE = 20;
    private static final boolean EASTMONEY_RANK_ENRICHMENT_ENABLED =
            Boolean.getBoolean("stockcrawler.rank.enrichEastMoney");

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取个股人气榜列表
     *
     * @param pageSize 每页数量 (最大 100)
     * @return 人气榜列表
     */
    public List<StockRankItem> getRankList(int pageSize) throws IOException {
        int safePageSize = StockCodeUtils.clamp(pageSize, 1, 100);
        log.info("Fetching stock rank list, pageSize: {}", safePageSize);

        String jsonBody = String.format(
                "{\"appId\":\"appId\",\"globalId\":\"\",\"pageSize\":%d,\"pageNum\":1}",
                safePageSize
        );

        Map<String, String> headers = Map.of(
                "Content-Type", "application/json",
                "Referer", "https://guba.eastmoney.com/rank/"
        );

        String response = HttpUtils.post(
                RANK_LIST_API,
                jsonBody,
                headers,
                CrawlerRequestPolicy.backgroundNews());
        List<StockRankItem> items = parseRankList(response);

        // 获取股票详细信息（名称、价格等）
        if (!items.isEmpty()) {
            fillStockInfo(items);
        }

        log.info("Got {} stock rank items", items.size());
        return items;
    }

    /**
     * 获取个股人气榜列表（默认 100 条）
     */
    public List<StockRankItem> getRankList() throws IOException {
        return getRankList(100);
    }

    /**
     * 获取 Top N 人气股
     */
    public List<StockRankItem> getTopN(int n) throws IOException {
        int safeN = StockCodeUtils.clamp(n, 1, 100);
        return getRankList(safeN);
    }

    /**
     * 解析人气榜列表
     */
    private List<StockRankItem> parseRankList(String json) throws IOException {
        JsonNode root = objectMapper.readTree(json);
        List<StockRankItem> items = new ArrayList<>();

        JsonNode dataNode = root.path("data");
        if (dataNode.isArray()) {
            for (JsonNode node : dataNode) {
                StockRankItem item = new StockRankItem();
                item.setStockCode(textValue(node, "sc"));
                item.setRank(node.path("rk").asInt(0));
                item.setRankChange(node.path("rc").asInt(0));
                item.setHistRankChange(node.path("hisRc").asInt(0));
                items.add(item);
            }
        }

        return items;
    }

    /**
     * 填充股票详细信息（名称、价格）
     * 优先使用腾讯财经（稳定不封 IP），失败后回退到东方财富
     */
    private void fillStockInfo(List<StockRankItem> items) {
        if (items.isEmpty()) {
            return;
        }
        try {
            int resolvedCount = fillStockInfoFromTencent(items);
            if (hasMissingStockInfo(items)) {
                fillMissingStockInfoFromEastMoneyIfEnabled(items, resolvedCount);
            }
        } catch (IOException ex) {
            log.warn("stock_rank_tencent_failed message={}", ex.getMessage());
            fillMissingStockInfoFromEastMoneyIfEnabled(items, 0);
        }
    }

    private void fillMissingStockInfoFromEastMoneyIfEnabled(List<StockRankItem> items, int resolvedCount) {
        if (!EASTMONEY_RANK_ENRICHMENT_ENABLED) {
            log.warn("stock_rank_tencent_incomplete resolvedCount={} itemCount={}, eastmoney_enrichment=disabled",
                    resolvedCount, items.size());
            return;
        }
        try {
            fillStockInfoFromEastMoney(items);
        } catch (IOException ex) {
            log.warn("stock_rank_eastmoney_enrichment_failed resolvedCount={} itemCount={} message={}",
                    resolvedCount, items.size(), ex.getMessage());
        }
    }

    /**
     * 通过东方财富批量获取股票名称和价格
     */
    private void fillStockInfoFromEastMoney(List<StockRankItem> items) throws IOException {
        // 构建股票代码列表 (格式: m:1+t:23,m:0+t:23 表示沪深A股)
        StringBuilder codes = new StringBuilder();
        for (StockRankItem item : items) {
            if (codes.length() > 0) {
                codes.append(",");
            }
            // 转换代码格式: SH601016 -> 1.601016, SZ000001 -> 0.000001
            String code = item.getStockCode();
            if (code != null) {
                if (code.startsWith("SH")) {
                    codes.append("1.").append(code.substring(2));
                } else if (code.startsWith("SZ")) {
                    codes.append("0.").append(code.substring(2));
                }
            }
        }

        String url = STOCK_INFO_API + "?fltt=2&secids=" + codes + "&fields=f12,f14,f2,f3";
        log.debug("Fetching stock info from eastmoney: {}", url);

        Map<String, String> headers = Map.of(
                "Referer", "https://guba.eastmoney.com/rank/"
        );

        String response = HttpUtils.getEastMoney(
                url, headers, CrawlerRequestPolicy.backgroundNews());
        parseStockInfo(response, items);
    }

    /**
     * 通过腾讯财经批量获取股票名称和价格（备用）
     */
    private int fillStockInfoFromTencent(List<StockRankItem> items) throws IOException {
        List<String> codes = new ArrayList<>();
        for (StockRankItem item : items) {
            String code = item.getStockCode();
            if (code != null) {
                codes.add(code.toLowerCase());
            }
        }

        int resolvedCount = 0;
        for (int start = 0; start < codes.size(); start += TENCENT_BATCH_SIZE) {
            List<String> batch = codes.subList(start, Math.min(start + TENCENT_BATCH_SIZE, codes.size()));
            String url = String.format(TENCENT_QUOTE_URL, String.join(",", batch));
            log.debug("Fetching stock info from tencent: {}", url);

            String response = HttpUtils.getWithCharset(
                    url, HttpUtils.GBK, CrawlerRequestPolicy.backgroundNews());
            resolvedCount += parseTencentStockInfo(response, items);
        }
        return resolvedCount;
    }

    /**
     * 解析腾讯财经返回的股票信息
     */
    private int parseTencentStockInfo(String body, List<StockRankItem> items) {
        Map<String, StockRankItem> itemMap = new HashMap<>();
        for (StockRankItem item : items) {
            if (item.getStockCode() != null) {
                itemMap.put(item.getStockCode(), item);
            }
        }

        int resolvedCount = 0;
        String[] lines = body.split(";");
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) continue;
            line = line.trim();
            try {
                int eqIdx = line.indexOf("=\"");
                if (eqIdx == -1) continue;

                // v_sh600519 -> SH600519
                String lookupCode = line.substring(2, eqIdx).toUpperCase();
                if (!lookupCode.startsWith("SH") && !lookupCode.startsWith("SZ")) continue;

                int dataStart = eqIdx + 2;
                int dataEnd = line.lastIndexOf("\"");
                if (dataEnd <= dataStart) continue;
                String data = line.substring(dataStart, dataEnd);
                if (data.isEmpty()) continue;

                String[] fields = data.split("~", -1);
                StockRankItem item = itemMap.get(lookupCode);
                if (item != null && fields.length > 32) {
                    item.setStockName(fields[1]);                          // 名称
                    item.setClosePrice(ParseUtils.parseDouble(fields[3])); // 当前价
                    item.setChangePercent(ParseUtils.parseDouble(fields[32])); // 涨跌幅
                    resolvedCount++;
                }
            } catch (RuntimeException ex) {
                log.warn("tencent_stock_info_parse_failed message={}", ex.getMessage());
            }
        }
        return resolvedCount;
    }

    private boolean hasMissingStockInfo(List<StockRankItem> items) {
        for (StockRankItem item : items) {
            if (item.getStockName() == null || item.getStockName().isBlank()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析股票信息
     */
    private void parseStockInfo(String json, List<StockRankItem> items) throws IOException {
        JsonNode root = objectMapper.readTree(json);
        JsonNode dataNode = root.path("data").path("diff");

        if (!dataNode.isArray()) {
            log.warn("Stock info API returned empty or unexpected 'diff' field. " +
                    "Stock names and prices may not be filled. items count: {}", items.size());
            return;
        }

        // 创建代码到信息的映射
        Map<String, StockRankItem> itemMap = new java.util.HashMap<>();
        for (StockRankItem item : items) {
            itemMap.put(item.getPureCode(), item);
        }

        for (JsonNode node : dataNode) {
            String code = textValue(node, "f12");  // 股票代码
            String name = textValue(node, "f14");  // 股票名称
            Double price = node.path("f2").asDouble(0);   // 最新价
            Double change = node.path("f3").asDouble(0);  // 涨跌幅

            StockRankItem item = itemMap.get(code);
            if (item != null) {
                item.setStockName(name);
                item.setClosePrice(price);
                item.setChangePercent(change);
            }
        }
    }

    private String textValue(JsonNode parent, String fieldName) {
        JsonNode value = parent == null ? null : parent.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }
}
