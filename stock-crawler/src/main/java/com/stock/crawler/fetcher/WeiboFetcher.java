package com.stock.crawler.fetcher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.crawler.model.HotItem;
import com.stock.crawler.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 微博热搜获取器
 */
public class WeiboFetcher implements Fetcher {

    private static final Logger log = LoggerFactory.getLogger(WeiboFetcher.class);
    private static final String WEIBO_HOT_SEARCH_URL = "https://weibo.com/ajax/side/hotSearch";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String platform() {
        return "weibo";
    }

    @Override
    public String platformCN() {
        return "微博热搜";
    }

    @Override
    public List<HotItem> fetch() throws IOException {
        log.info("Fetching weibo hot search");

        Map<String, String> headers = Map.of(
                "Accept", "application/json, text/plain, */*",
                "Referer", "https://weibo.com/"
        );

        String json = HttpUtils.get(WEIBO_HOT_SEARCH_URL, headers);
        return parseWeiboResponse(json);
    }

    private List<HotItem> parseWeiboResponse(String json) throws IOException {
        JsonNode root = objectMapper.readTree(json);

        int ok = root.path("ok").asInt(0);
        if (ok != 1) {
            throw new IOException("Weibo API error: ok=" + ok);
        }

        List<HotItem> items = new ArrayList<>();
        JsonNode realtimeNode = root.path("data").path("realtime");

        if (realtimeNode.isArray()) {
            int rank = 1;
            for (JsonNode node : realtimeNode) {
                JsonNode wordNode = node.get("word");
                String word = wordNode == null || wordNode.isNull() ? null : wordNode.asText();
                if (word == null || word.isEmpty()) {
                    continue;
                }

                int num = node.path("num").asInt(0);
                String searchUrl = "https://s.weibo.com/weibo?q=" +
                        URLEncoder.encode(word, StandardCharsets.UTF_8);

                HotItem item = new HotItem(
                        "weibo_" + rank,
                        word,
                        searchUrl,
                        rank,
                        "weibo"
                );
                item.setHotScore(num);

                items.add(item);
                rank++;
                if (rank > 50) {
                    break;
                }
            }
        }

        log.info("Fetched {} weibo hot items", items.size());
        return items;
    }
}
