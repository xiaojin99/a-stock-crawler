package com.stock.crawler.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.crawler.model.LongHuBangDetail;
import com.stock.crawler.model.LongHuBangItem;
import com.stock.crawler.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 龙虎榜服务
 * 调用东方财富龙虎榜 API
 */
public class LongHuBangService {

    private static final Logger log = LoggerFactory.getLogger(LongHuBangService.class);

    // 龙虎榜列表 API
    private static final String LHB_LIST_URL_TEMPLATE =
            "https://datacenter-web.eastmoney.com/api/data/v1/get?" +
                    "sortColumns=TRADE_DATE,BILLBOARD_NET_AMT&sortTypes=-1,-1" +
                    "&pageSize=%d&pageNumber=%d" +
                    "&reportName=RPT_DAILYBILLBOARD_DETAILSNEW" +
                    "&columns=SECURITY_CODE,SECUCODE,SECURITY_NAME_ABBR,TRADE_DATE,EXPLAIN,CLOSE_PRICE,CHANGE_RATE," +
                    "BILLBOARD_NET_AMT,BILLBOARD_BUY_AMT,BILLBOARD_SELL_AMT,BILLBOARD_DEAL_AMT,ACCUM_AMOUNT," +
                    "DEAL_NET_RATIO,DEAL_AMOUNT_RATIO,TURNOVERRATE,FREE_MARKET_CAP,EXPLANATION," +
                    "D1_CLOSE_ADJCHRATE,D2_CLOSE_ADJCHRATE,D5_CLOSE_ADJCHRATE,D10_CLOSE_ADJCHRATE,SECURITY_TYPE_CODE" +
                    "&source=WEB&client=WEB";

    // 龙虎榜买入明细 API
    private static final String LHB_BUY_DETAIL_URL_TEMPLATE =
            "https://datacenter-web.eastmoney.com/api/data/v1/get?" +
                    "reportName=RPT_BILLBOARD_DAILYDETAILSBUY&columns=ALL" +
                    "&filter=(TRADE_DATE%%3D%%27%s%%27)(SECURITY_CODE%%3D%%22%s%%22)" +
                    "&pageNumber=1&pageSize=50&sortTypes=-1&sortColumns=BUY&source=WEB&client=WEB";

    // 龙虎榜卖出明细 API
    private static final String LHB_SELL_DETAIL_URL_TEMPLATE =
            "https://datacenter-web.eastmoney.com/api/data/v1/get?" +
                    "reportName=RPT_BILLBOARD_DAILYDETAILSSELL&columns=ALL" +
                    "&filter=(TRADE_DATE%%3D%%27%s%%27)(SECURITY_CODE%%3D%%22%s%%22)" +
                    "&pageNumber=1&pageSize=50&sortTypes=-1&sortColumns=SELL&source=WEB&client=WEB";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private static class CacheEntry {
        final List<LongHuBangItem> items;
        final int total;
        final LocalDateTime timestamp;

        CacheEntry(List<LongHuBangItem> items, int total) {
            this.items = items;
            this.total = total;
            this.timestamp = LocalDateTime.now();
        }

        boolean isExpired() {
            return Duration.between(timestamp, LocalDateTime.now()).compareTo(CACHE_TTL) > 0;
        }
    }

    /**
     * 龙虎榜列表结果
     */
    public static class LongHuBangResult {
        private List<LongHuBangItem> items;
        private int total;

        public List<LongHuBangItem> getItems() {
            return items;
        }

        public void setItems(List<LongHuBangItem> items) {
            this.items = items;
        }

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }
    }

    /**
     * 获取龙虎榜列表
     *
     * @param pageSize   每页数量 (默认 50，最大 200)
     * @param pageNumber 页码 (从 1 开始)
     * @param tradeDate  交易日期 (格式 YYYY-MM-DD，可为空)
     */
    public LongHuBangResult getLongHuBangList(int pageSize, int pageNumber, String tradeDate) throws IOException {
        if (pageSize <= 0) pageSize = 50;
        if (pageSize > 200) pageSize = 200;
        if (pageNumber <= 0) pageNumber = 1;

        String cacheKey = pageSize + "_" + pageNumber + "_" + tradeDate;

        // 检查缓存
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            LongHuBangResult result = new LongHuBangResult();
            result.setItems(new ArrayList<>(cached.items));
            result.setTotal(cached.total);
            return result;
        }

        // 从 API 获取
        LongHuBangResult result = fetchLongHuBangList(pageSize, pageNumber, tradeDate);

        // 更新缓存
        cache.put(cacheKey, new CacheEntry(result.getItems(), result.getTotal()));

        return result;
    }

    /**
     * 获取龙虎榜列表（默认参数）
     */
    public LongHuBangResult getLongHuBangList() throws IOException {
        return getLongHuBangList(50, 1, null);
    }

    /**
     * 从东方财富 API 获取龙虎榜数据
     */
    private LongHuBangResult fetchLongHuBangList(int pageSize, int pageNumber, String tradeDate) throws IOException {
        String url = String.format(LHB_LIST_URL_TEMPLATE, pageSize, pageNumber);

        // 添加日期筛选
        if (tradeDate != null && !tradeDate.isEmpty()) {
            url += String.format("&filter=(TRADE_DATE%%3D%%27%s%%27)", tradeDate);
        }

        log.info("Fetching longhubang list: pageSize={}, pageNo={}, date={}", pageSize, pageNumber, tradeDate);

        Map<String, String> headers = Map.of("Referer", "https://data.eastmoney.com/");
        String json = HttpUtils.getEastMoney(url, headers);

        return parseLongHuBangResponse(json);
    }

    /**
     * 解析龙虎榜 API 响应
     */
    private LongHuBangResult parseLongHuBangResponse(String json) throws IOException {
        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IOException("Failed to parse longhubang JSON: " + e.getMessage(), e);
        }

        boolean success = root.path("success").asBoolean(false);
        if (!success) {
            String message = textValue(root, "message", "Unknown error");
            String snippet = json.length() > 200 ? json.substring(0, 200) + "..." : json;
            log.warn("获取龙虎榜数据失败, message={}, json片段={}", message, snippet);
            throw new IOException("获取龙虎榜数据失败: " + message);
        }

        LongHuBangResult result = new LongHuBangResult();
        List<LongHuBangItem> items = new ArrayList<>();

        JsonNode dataNode = root.path("result").path("data");
        int total = root.path("result").path("count").asInt(0);

        if (dataNode.isArray()) {
            for (JsonNode node : dataNode) {
                LongHuBangItem item = new LongHuBangItem();

                // 解析日期 "2026-02-09 00:00:00" -> "2026-02-09"
                String tradeDate = textValue(node, "TRADE_DATE");
                if (tradeDate != null && tradeDate.length() > 10) {
                    tradeDate = tradeDate.substring(0, 10);
                }
                item.setTradeDate(tradeDate);

                item.setCode(textValue(node, "SECURITY_CODE"));
                item.setSecuCode(textValue(node, "SECUCODE"));
                item.setName(textValue(node, "SECURITY_NAME_ABBR"));
                item.setClosePrice(node.path("CLOSE_PRICE").asDouble(0));
                item.setChangePercent(node.path("CHANGE_RATE").asDouble(0));
                item.setNetBuyAmt(node.path("BILLBOARD_NET_AMT").asDouble(0));
                item.setBuyAmt(node.path("BILLBOARD_BUY_AMT").asDouble(0));
                item.setSellAmt(node.path("BILLBOARD_SELL_AMT").asDouble(0));
                item.setTotalAmt(node.path("BILLBOARD_DEAL_AMT").asDouble(0));
                item.setAccumAmount(node.path("ACCUM_AMOUNT").asDouble(0));
                item.setNetRatio(node.path("DEAL_NET_RATIO").asDouble(0));
                item.setDealRatio(node.path("DEAL_AMOUNT_RATIO").asDouble(0));
                item.setTurnoverRate(node.path("TURNOVERRATE").asDouble(0));
                item.setFreeCap(node.path("FREE_MARKET_CAP").asDouble(0));
                item.setReason(textValue(node, "EXPLAIN"));
                item.setReasonDetail(textValue(node, "EXPLANATION"));
                item.setD1Change(node.path("D1_CLOSE_ADJCHRATE").asDouble(0));
                item.setD2Change(node.path("D2_CLOSE_ADJCHRATE").asDouble(0));
                item.setD5Change(node.path("D5_CLOSE_ADJCHRATE").asDouble(0));
                item.setD10Change(node.path("D10_CLOSE_ADJCHRATE").asDouble(0));

                items.add(item);
            }
        }

        result.setItems(items);
        result.setTotal(total);

        log.info("Fetched {} longhubang items, total: {}", items.size(), total);
        return result;
    }

    /**
     * 获取个股龙虎榜营业部明细
     *
     * @param code      股票代码
     * @param tradeDate 交易日期 (格式 YYYY-MM-DD)
     */
    public List<LongHuBangDetail> getStockDetail(String code, String tradeDate) throws IOException {
        List<LongHuBangDetail> buyDetails = fetchDetail(code, tradeDate, "buy");
        List<LongHuBangDetail> sellDetails = fetchDetail(code, tradeDate, "sell");

        List<LongHuBangDetail> result = new ArrayList<>();
        result.addAll(buyDetails);
        result.addAll(sellDetails);
        return result;
    }

    /**
     * 获取营业部明细（买入或卖出）
     */
    private List<LongHuBangDetail> fetchDetail(String code, String tradeDate, String direction) throws IOException {
        String urlTemplate = "buy".equals(direction) ? LHB_BUY_DETAIL_URL_TEMPLATE : LHB_SELL_DETAIL_URL_TEMPLATE;
        String url = String.format(urlTemplate, tradeDate, code);

        log.info("Fetching longhubang detail: code={}, date={}, direction={}", code, tradeDate, direction);

        Map<String, String> headers = Map.of("Referer", "https://data.eastmoney.com/");
        String json = HttpUtils.getEastMoney(url, headers);

        return parseDetailResponse(json, direction);
    }

    /**
     * 解析营业部明细响应
     */
    private List<LongHuBangDetail> parseDetailResponse(String json, String direction) throws IOException {
        JsonNode root = objectMapper.readTree(json);

        boolean success = root.path("success").asBoolean(false);
        if (!success) {
            return new ArrayList<>();
        }

        List<LongHuBangDetail> items = new ArrayList<>();
        JsonNode dataNode = root.path("result").path("data");

        if (dataNode.isArray()) {
            int rank = 1;
            for (JsonNode node : dataNode) {
                LongHuBangDetail detail = new LongHuBangDetail();
                detail.setRank(rank++);
                detail.setOperName(textValue(node, "OPERATEDEPT_NAME"));
                detail.setBuyAmt(node.path("BUY").asDouble(0));
                detail.setSellAmt(node.path("SELL").asDouble(0));
                detail.setNetAmt(node.path("NET").asDouble(0));
                detail.setBuyPercent(node.path("TOTAL_BUYRIO").asDouble(0));
                detail.setSellPercent(node.path("TOTAL_SELLRIO").asDouble(0));
                detail.setDirection(direction);

                items.add(detail);
            }
        }

        return items;
    }

    private String textValue(JsonNode parent, String fieldName) {
        return textValue(parent, fieldName, null);
    }

    private String textValue(JsonNode parent, String fieldName, String defaultValue) {
        JsonNode value = parent == null ? null : parent.get(fieldName);
        return value == null || value.isNull() ? defaultValue : value.asText();
    }
}
