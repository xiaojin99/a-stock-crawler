package com.stock.crawler.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.crawler.model.AnnouncementItem;
import com.stock.crawler.model.ConceptBlock;
import com.stock.crawler.model.ConceptBlockResult;
import com.stock.crawler.model.FundFlowPoint;
import com.stock.crawler.model.MarketNewsItem;
import com.stock.crawler.model.StockBasicInfo;
import com.stock.crawler.util.CrawlerRequestPolicy;
import com.stock.crawler.util.HttpUtils;
import com.stock.crawler.util.ParseUtils;
import com.stock.crawler.util.StockCodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 个股分析素材服务。
 *
 * <p>补齐 a-stock-data 中高价值、低封锁风险或东财独有的数据：
 * 板块归因、资金流、个股新闻、全球快讯、基本面和巨潮公告。</p>
 */
public class StockIntelligenceService {

    private static final Logger log = LoggerFactory.getLogger(StockIntelligenceService.class);

    private static final String EASTMONEY_SLIST_URL = "https://push2.eastmoney.com/api/qt/slist/get";
    private static final String EASTMONEY_FFLOW_MINUTE_URL = "https://push2.eastmoney.com/api/qt/stock/fflow/kline/get";
    private static final String EASTMONEY_FFLOW_DAILY_URL = "https://push2his.eastmoney.com/api/qt/stock/fflow/daykline/get";
    private static final String SINA_FFLOW_DAILY_URL =
            "https://money.finance.sina.com.cn/quotes_service/api/json_v2.php/MoneyFlow.ssl_qsfx_lscjfb";
    private static final String EASTMONEY_STOCK_NEWS_URL = "https://search-api-web.eastmoney.com/search/jsonp";
    private static final String EASTMONEY_GLOBAL_NEWS_URL = "https://np-weblist.eastmoney.com/comm/web/getFastNewsList";
    private static final String EASTMONEY_STOCK_INFO_URL = "https://push2.eastmoney.com/api/qt/stock/get";
    private static final String CNINFO_STOCK_MAP_URL = "http://www.cninfo.com.cn/new/data/szse_stock.json";
    private static final String CNINFO_ANNOUNCEMENT_URL = "https://www.cninfo.com.cn/new/hisAnnouncement/query";
    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile Map<String, String> cninfoOrgIdMap = Map.of();

    /**
     * 个股所属行业/概念/地域板块。
     */
    public ConceptBlockResult getConceptBlocks(String stockCode) throws IOException {
        String code = normalizeCode(stockCode);
        Map<String, String> params = new LinkedHashMap<>();
        params.put("fltt", "2");
        params.put("invt", "2");
        params.put("secid", marketCode(code) + "." + code);
        params.put("spt", "3");
        params.put("pi", "0");
        params.put("pz", "200");
        params.put("po", "1");
        params.put("fields", "f12,f14,f3,f128");

        String json = HttpUtils.getEastMoney(
                EASTMONEY_SLIST_URL + "?" + buildQuery(params),
                Map.of("Referer", "https://quote.eastmoney.com/"),
                CrawlerRequestPolicy.interactive());
        JsonNode root = objectMapper.readTree(json);
        JsonNode diff = root.path("data").path("diff");

        List<ConceptBlock> boards = new ArrayList<>();
        if (diff.isObject()) {
            diff.fields().forEachRemaining(entry -> boards.add(parseConceptBlock(entry.getValue())));
        } else if (diff.isArray()) {
            for (JsonNode item : diff) {
                boards.add(parseConceptBlock(item));
            }
        }

        List<String> tags = boards.stream()
                .map(ConceptBlock::getName)
                .filter(name -> name != null && !name.isBlank())
                .toList();
        ConceptBlockResult result = new ConceptBlockResult();
        result.setBoards(boards);
        result.setConceptTags(tags);
        result.setTotal(boards.size());
        return result;
    }

    /**
     * 分钟级个股资金流。
     */
    public List<FundFlowPoint> getFundFlowMinute(String stockCode) throws IOException {
        String code = normalizeCode(stockCode);
        Map<String, String> params = new LinkedHashMap<>();
        params.put("secid", marketCode(code) + "." + code);
        params.put("lmt", "0");
        params.put("klt", "1");
        params.put("fields1", "f1,f2,f3,f7");
        params.put("fields2", "f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61,f62,f63,f64,f65");
        params.put("ut", "b2884a393a59ad64002292a3e90d46a5");

        String json = HttpUtils.getEastMoney(
                EASTMONEY_FFLOW_MINUTE_URL + "?" + buildQuery(params),
                quoteHeaders(),
                CrawlerRequestPolicy.interactive());
        JsonNode root = objectMapper.readTree(json);
        return parseFundFlowLines(root.path("data").path("klines"));
    }

    /**
     * 日级个股资金流，默认最近 120 个交易日。
     */
    public List<FundFlowPoint> getFundFlowDaily(String stockCode, int limit) throws IOException {
        String code = normalizeCode(stockCode);
        int safeLimit = Math.max(1, Math.min(limit, 120));
        Map<String, String> params = new LinkedHashMap<>();
        params.put("secid", marketCode(code) + "." + code);
        params.put("klt", "101");
        params.put("fields1", "f1,f2,f3,f7");
        params.put("fields2", "f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61,f62,f63,f64,f65");
        params.put("lmt", String.valueOf(safeLimit));
        params.put("ut", "b2884a393a59ad64002292a3e90d46a5");

        try {
            String json = HttpUtils.getEastMoney(
                    EASTMONEY_FFLOW_DAILY_URL + "?" + buildQuery(params),
                    quoteHeaders(),
                    CrawlerRequestPolicy.interactive());
            JsonNode root = objectMapper.readTree(json);
            List<FundFlowPoint> points = parseFundFlowLines(root.path("data").path("klines"));
            if (!points.isEmpty()) {
                return points;
            }
            log.warn("eastmoney_fund_flow_daily_empty stockCode={} fallback=sina", code);
        } catch (IOException exception) {
            log.warn("eastmoney_fund_flow_daily_failed stockCode={} fallback=sina message={}",
                    code, exception.getMessage());
        }
        return getSinaFundFlowDaily(code, safeLimit);
    }

    /**
     * 东财个股新闻。
     */
    public List<MarketNewsItem> getStockNews(String stockCode, int pageSize) throws IOException {
        String code = normalizeCode(stockCode);
        int safePageSize = Math.max(1, Math.min(pageSize, 50));
        String param = """
                {"uid":"","keyword":"%s","type":["cmsArticleWebOld"],"client":"web","clientType":"web","clientVersion":"curr","param":{"cmsArticleWebOld":{"searchScope":"default","sort":"default","pageIndex":1,"pageSize":%d,"preTag":"","postTag":""}}}
                """.formatted(code, safePageSize).trim();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("cb", "jQuery_news");
        params.put("param", param);

        String text = HttpUtils.getEastMoney(
                EASTMONEY_STOCK_NEWS_URL + "?" + buildQuery(params),
                Map.of("Referer", "https://so.eastmoney.com/"),
                CrawlerRequestPolicy.backgroundNews());
        JsonNode root = objectMapper.readTree(stripJsonp(text));
        JsonNode articles = root.path("result").path("cmsArticleWebOld");
        List<MarketNewsItem> results = new ArrayList<>();
        if (!articles.isArray()) {
            return results;
        }
        for (JsonNode article : articles) {
            results.add(MarketNewsItem.builder()
                    .title(cleanText(text(article, "title")))
                    .content(limit(cleanText(text(article, "content")), 220))
                    .time(text(article, "date"))
                    .source(text(article, "mediaName", "东方财富"))
                    .url(text(article, "url"))
                    .build());
        }
        return results;
    }

    /**
     * 东方财富 7x24 全球财经快讯。
     */
    public List<MarketNewsItem> getGlobalNews(int pageSize) throws IOException {
        int safePageSize = Math.max(1, Math.min(pageSize, 100));
        Map<String, String> params = new LinkedHashMap<>();
        params.put("client", "web");
        params.put("biz", "web_724");
        params.put("fastColumn", "102");
        params.put("sortEnd", "");
        params.put("pageSize", String.valueOf(safePageSize));
        params.put("req_trace", UUID.randomUUID().toString());

        String json = HttpUtils.getEastMoney(
                EASTMONEY_GLOBAL_NEWS_URL + "?" + buildQuery(params),
                Map.of("Referer", "https://kuaixun.eastmoney.com/"),
                CrawlerRequestPolicy.backgroundNews());
        JsonNode root = objectMapper.readTree(json);
        JsonNode list = root.path("data").path("fastNewsList");
        List<MarketNewsItem> results = new ArrayList<>();
        if (!list.isArray()) {
            return results;
        }
        for (JsonNode item : list) {
            results.add(MarketNewsItem.builder()
                    .title(text(item, "title"))
                    .content(limit(text(item, "summary"), 220))
                    .time(text(item, "showTime"))
                    .source("东方财富")
                    .url(text(item, "url"))
                    .build());
        }
        return results;
    }

    /**
     * 东财个股基本面信息。
     */
    public StockBasicInfo getStockBasicInfo(String stockCode) throws IOException {
        String code = normalizeCode(stockCode);
        Map<String, String> params = new LinkedHashMap<>();
        params.put("fltt", "2");
        params.put("invt", "2");
        params.put("fields", "f57,f58,f84,f85,f127,f116,f117,f189,f43");
        params.put("secid", marketCode(code) + "." + code);

        String json = HttpUtils.getEastMoney(
                EASTMONEY_STOCK_INFO_URL + "?" + buildQuery(params),
                quoteHeaders(),
                CrawlerRequestPolicy.interactive());
        JsonNode data = objectMapper.readTree(json).path("data");
        StockBasicInfo info = new StockBasicInfo();
        info.setCode(text(data, "f57"));
        info.setName(text(data, "f58"));
        info.setIndustry(text(data, "f127"));
        info.setTotalShares(ParseUtils.parseBigDecimal(text(data, "f84", "0")));
        info.setFloatShares(ParseUtils.parseBigDecimal(text(data, "f85", "0")));
        info.setMarketCap(ParseUtils.parseBigDecimal(text(data, "f116", "0")));
        info.setFloatMarketCap(ParseUtils.parseBigDecimal(text(data, "f117", "0")));
        info.setListDate(formatListDate(text(data, "f189")));
        info.setPrice(ParseUtils.parseBigDecimal(text(data, "f43", "0")));
        return info;
    }

    /**
     * 巨潮公告全文检索。
     */
    public List<AnnouncementItem> getAnnouncements(String stockCode, int pageSize) throws IOException {
        String code = normalizeCode(stockCode);
        int safePageSize = Math.max(1, Math.min(pageSize, 50));
        Map<String, String> form = new LinkedHashMap<>();
        form.put("stock", code + "," + getCninfoOrgId(code));
        form.put("tabName", "fulltext");
        form.put("pageSize", String.valueOf(safePageSize));
        form.put("pageNum", "1");
        form.put("column", "");
        form.put("category", "");
        form.put("plate", "");
        form.put("seDate", "");
        form.put("searchkey", "");
        form.put("secid", "");
        form.put("sortName", "");
        form.put("sortType", "");
        form.put("isHLtitle", "true");

        String json = HttpUtils.postForm(
                CNINFO_ANNOUNCEMENT_URL,
                form,
                Map.of(
                        "Referer", "https://www.cninfo.com.cn/new/disclosure",
                        "Origin", "https://www.cninfo.com.cn"),
                CrawlerRequestPolicy.backgroundNews());
        return parseAnnouncements(json);
    }

    List<AnnouncementItem> parseAnnouncements(String json) throws IOException {
        JsonNode announcements = objectMapper.readTree(json).path("announcements");
        List<AnnouncementItem> results = new ArrayList<>();
        if (!announcements.isArray()) {
            return results;
        }
        for (JsonNode item : announcements) {
            String announcementId = text(item, "announcementId");
            String adjunctUrl = text(item, "adjunctUrl");
            results.add(AnnouncementItem.builder()
                    .title(cleanText(text(item, "announcementTitle")))
                    .type(text(item, "announcementTypeName"))
                    .date(formatCninfoDate(item.path("announcementTime")))
                    .url(resolveAnnouncementUrl(adjunctUrl, announcementId))
                    .build());
        }
        return results;
    }

    List<FundFlowPoint> parseSinaDailyFundFlow(String json) throws IOException {
        JsonNode rows = objectMapper.readTree(json);
        List<FundFlowPoint> results = new ArrayList<>();
        if (!rows.isArray()) {
            return results;
        }
        for (JsonNode row : rows) {
            BigDecimal superNet = ParseUtils.parseBigDecimal(text(row, "r0_net"));
            BigDecimal largeNet = ParseUtils.parseBigDecimal(text(row, "r1_net"));
            results.add(FundFlowPoint.builder()
                    .time(text(row, "opendate"))
                    .mainNet(superNet.add(largeNet))
                    .superNet(superNet)
                    .largeNet(largeNet)
                    .midNet(ParseUtils.parseBigDecimal(text(row, "r2_net")))
                    .smallNet(ParseUtils.parseBigDecimal(text(row, "r3_net")))
                    .build());
        }
        return results;
    }

    private List<FundFlowPoint> getSinaFundFlowDaily(String code, int limit) throws IOException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("daima", sinaMarketCode(code));
        params.put("page", "1");
        params.put("num", String.valueOf(limit));
        params.put("sort", "opendate");
        params.put("asc", "0");
        String json = HttpUtils.get(
                SINA_FFLOW_DAILY_URL + "?" + buildQuery(params),
                Map.of(
                        "Accept", "application/json, text/plain, */*",
                        "Referer", "https://money.finance.sina.com.cn/moneyflow/"),
                CrawlerRequestPolicy.interactive());
        return parseSinaDailyFundFlow(json);
    }

    private String sinaMarketCode(String code) {
        return (code.startsWith("6") ? "sh" : "sz") + code;
    }

    private String resolveAnnouncementUrl(String adjunctUrl, String announcementId) {
        if (adjunctUrl != null && !adjunctUrl.isBlank()) {
            String path = adjunctUrl.startsWith("/") ? adjunctUrl.substring(1) : adjunctUrl;
            return "https://static.cninfo.com.cn/" + path;
        }
        if (announcementId == null || announcementId.isBlank()) {
            return "";
        }
        return "https://www.cninfo.com.cn/new/disclosure/detail?announcementId="
                + encode(announcementId);
    }

    private ConceptBlock parseConceptBlock(JsonNode item) {
        return ConceptBlock.builder()
                .name(text(item, "f14"))
                .code(text(item, "f12"))
                .changePercent(ParseUtils.parseBigDecimal(text(item, "f3", "0")))
                .leadStock(text(item, "f128"))
                .build();
    }

    private List<FundFlowPoint> parseFundFlowLines(JsonNode klines) {
        List<FundFlowPoint> results = new ArrayList<>();
        if (!klines.isArray()) {
            return results;
        }
        for (JsonNode node : klines) {
            String[] parts = nodeText(node, "").split(",");
            if (parts.length < 6) {
                continue;
            }
            results.add(FundFlowPoint.builder()
                    .time(parts[0])
                    .mainNet(ParseUtils.parseBigDecimal(parts[1]))
                    .smallNet(ParseUtils.parseBigDecimal(parts[2]))
                    .midNet(ParseUtils.parseBigDecimal(parts[3]))
                    .largeNet(ParseUtils.parseBigDecimal(parts[4]))
                    .superNet(ParseUtils.parseBigDecimal(parts[5]))
                    .build());
        }
        return results;
    }

    private String getCninfoOrgId(String code) {
        Map<String, String> mapping = cninfoOrgIdMap;
        if (mapping.isEmpty()) {
            synchronized (this) {
                if (cninfoOrgIdMap.isEmpty()) {
                    cninfoOrgIdMap = fetchCninfoOrgIdMap();
                }
                mapping = cninfoOrgIdMap;
            }
        }
        String orgId = mapping.get(code);
        if (orgId != null && !orgId.isBlank()) {
            return orgId;
        }
        if (code.startsWith("6")) {
            return "gssh0" + code;
        }
        if (code.startsWith("8") || code.startsWith("4")) {
            return "gsbj0" + code;
        }
        return "gssz0" + code;
    }

    private Map<String, String> fetchCninfoOrgIdMap() {
        try {
            String json = HttpUtils.get(
                    CNINFO_STOCK_MAP_URL,
                    Map.of("Referer", "https://www.cninfo.com.cn/"),
                    CrawlerRequestPolicy.backgroundNews());
            JsonNode stockList = objectMapper.readTree(json).path("stockList");
            Map<String, String> mapping = new LinkedHashMap<>();
            if (stockList.isArray()) {
                for (JsonNode item : stockList) {
                    String code = text(item, "code");
                    String orgId = text(item, "orgId");
                    if (!code.isBlank() && !orgId.isBlank()) {
                        mapping.put(code, orgId);
                    }
                }
            }
            return mapping;
        } catch (Exception ex) {
            log.warn("cninfo_orgid_map_fetch_failed message={}", ex.getMessage());
            return Map.of();
        }
    }

    private Map<String, String> quoteHeaders() {
        return Map.of(
                "Referer", "https://quote.eastmoney.com/",
                "Origin", "https://quote.eastmoney.com"
        );
    }

    private String buildQuery(Map<String, String> params) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!builder.isEmpty()) {
                builder.append('&');
            }
            builder.append(encode(entry.getKey()))
                    .append('=')
                    .append(encode(entry.getValue()));
        }
        return builder.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String stripJsonp(String text) {
        int left = text.indexOf('(');
        int right = text.lastIndexOf(')');
        if (left >= 0 && right > left) {
            return text.substring(left + 1, right);
        }
        return text;
    }

    private String normalizeCode(String stockCode) {
        return StockCodeUtils.stripMarket(stockCode);
    }

    private int marketCode(String code) {
        return code != null && code.startsWith("6") ? 1 : 0;
    }

    private String cleanText(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxLength);
    }

    private String formatListDate(String raw) {
        if (raw == null || raw.length() != 8) {
            return raw == null ? "" : raw;
        }
        return raw.substring(0, 4) + "-" + raw.substring(4, 6) + "-" + raw.substring(6, 8);
    }

    private String formatCninfoDate(JsonNode timestampNode) {
        if (timestampNode == null || timestampNode.isMissingNode() || timestampNode.isNull()) {
            return "";
        }
        if (timestampNode.isNumber()) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestampNode.asLong()), CHINA_ZONE)
                    .format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        String value = nodeText(timestampNode, "");
        return value.length() > 10 ? value.substring(0, 10) : value;
    }

    private String text(JsonNode parent, String fieldName) {
        return text(parent, fieldName, "");
    }

    private String text(JsonNode parent, String fieldName, String defaultValue) {
        return parent == null ? defaultValue : nodeText(parent.path(fieldName), defaultValue);
    }

    private String nodeText(JsonNode node, String defaultValue) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return defaultValue;
        }
        String value = node.asText();
        return value == null ? defaultValue : value;
    }
}
