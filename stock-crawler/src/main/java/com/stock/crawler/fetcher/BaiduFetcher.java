package com.stock.crawler.fetcher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.crawler.model.HotItem;
import com.stock.crawler.util.CrawlerRequestPolicy;
import com.stock.crawler.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 百度热搜获取器
 */
public class BaiduFetcher implements Fetcher {

    private static final Logger log = LoggerFactory.getLogger(BaiduFetcher.class);
    private static final String BAIDU_HOT_URL = "https://top.baidu.com/api/board?platform=wise&tab=realtime";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HotTrendHttpClient httpClient;

    public BaiduFetcher() {
        this(HttpUtils::get);
    }

    BaiduFetcher(HotTrendHttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    @Override
    public String platform() {
        return "baidu";
    }

    @Override
    public String platformCN() {
        return "百度热搜";
    }

    @Override
    public List<HotItem> fetch() throws IOException {
        log.info("Fetching baidu hot search");

        Map<String, String> headers = Map.of(
                "User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X)"
        );

        String json = httpClient.get(
                BAIDU_HOT_URL, headers, CrawlerRequestPolicy.backgroundNews());
        return parseBaiduResponse(json);
    }

    private List<HotItem> parseBaiduResponse(String json) throws IOException {
        JsonNode root = objectMapper.readTree(json);

        List<HotItem> items = new ArrayList<>();
        JsonNode cardsNode = root.path("data").path("cards");

        if (cardsNode.isArray()) {
            int rank = 1;
            for (JsonNode card : cardsNode) {
                JsonNode contentGroup = card.path("content");
                if (contentGroup.isArray()) {
                    for (JsonNode content : contentGroup) {
                        JsonNode contentItems = content.path("content");
                        if (contentItems.isArray()) {
                            for (JsonNode item : contentItems) {
                                String word = textValue(item, "word");
                                if (word == null || word.isEmpty()) {
                                    continue;
                                }

                                String url = textValue(item, "url", "");
                                if (url.isEmpty()) {
                                    url = "https://www.baidu.com/s?wd=" + word;
                                }

                                HotItem hotItem = new HotItem(
                                        "baidu_" + rank,
                                        word,
                                        url,
                                        rank,
                                        "baidu"
                                );

                                items.add(hotItem);
                                rank++;
                                if (rank > 50) {
                                    return items;
                                }
                            }
                        }
                    }
                }
            }
        }

        log.info("Fetched {} baidu hot items", items.size());
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
