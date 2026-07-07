package com.stock.web.controller;

import com.stock.crawler.model.GubaComment;
import com.stock.crawler.model.GubaPost;
import com.stock.crawler.model.HotItem;
import com.stock.crawler.model.KLineData;
import com.stock.crawler.model.LongHuBangItem;
import com.stock.crawler.model.MarketHotBoard;
import com.stock.crawler.model.MarketNewsItem;
import com.stock.crawler.model.ResearchReport;
import com.stock.crawler.model.StockQuote;
import com.stock.crawler.model.StockRankItem;
import com.stock.crawler.model.TechnicalIndicators;
import com.stock.crawler.model.Telegraph;
import com.stock.crawler.util.StockCodeUtils;
import com.stock.web.service.FinancialWebService;
import com.stock.web.service.GubaService;
import com.stock.web.service.HotTrendService;
import com.stock.web.service.LongHuBangService;
import com.stock.web.service.MarketNewsWebService;
import com.stock.web.service.NewsService;
import com.stock.web.service.ResearchReportService;
import com.stock.web.service.StockMarketWebService;
import com.stock.web.service.StockRankService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;

@Controller
public class HomeController {
    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    private final NewsService newsService;
    private final ResearchReportService reportService;
    private final HotTrendService hotTrendService;
    private final LongHuBangService longHuBangService;
    private final GubaService gubaService;
    private final StockRankService stockRankService;
    private final StockMarketWebService stockMarketWebService;
    private final MarketNewsWebService marketNewsWebService;
    private final FinancialWebService financialWebService;

    public HomeController(NewsService newsService,
                          ResearchReportService reportService,
                          HotTrendService hotTrendService,
                          LongHuBangService longHuBangService,
                          GubaService gubaService,
                          StockRankService stockRankService,
                          StockMarketWebService stockMarketWebService,
                          MarketNewsWebService marketNewsWebService,
                          FinancialWebService financialWebService) {
        this.newsService = newsService;
        this.reportService = reportService;
        this.hotTrendService = hotTrendService;
        this.longHuBangService = longHuBangService;
        this.gubaService = gubaService;
        this.stockRankService = stockRankService;
        this.stockMarketWebService = stockMarketWebService;
        this.marketNewsWebService = marketNewsWebService;
        this.financialWebService = financialWebService;
    }

    @GetMapping("/")
    public String index(Model model) {
        try {
            List<Telegraph> news = newsService.getTelegraphList();
            model.addAttribute("newsCount", news.size());
        } catch (Exception e) {
            log.warn("Failed to fetch news: {}", e.getMessage());
            model.addAttribute("newsCount", 0);
        }

        try {
            List<HotItem> weibo = hotTrendService.getWeiboHot();
            List<HotItem> baidu = hotTrendService.getBaiduHot();
            model.addAttribute("hotCount", weibo.size() + baidu.size());
        } catch (Exception e) {
            log.warn("Failed to fetch hot trends: {}", e.getMessage());
            model.addAttribute("hotCount", 0);
        }

        try {
            List<LongHuBangItem> lhb = longHuBangService.getLongHuBangList();
            model.addAttribute("lhbCount", lhb.size());
        } catch (Exception e) {
            log.warn("Failed to fetch longhubang: {}", e.getMessage());
            model.addAttribute("lhbCount", 0);
        }

        try {
            List<StockRankItem> rank = stockRankService.getRankList(50);
            model.addAttribute("rankCount", rank.size());
        } catch (Exception e) {
            log.warn("Failed to fetch rank: {}", e.getMessage());
            model.addAttribute("rankCount", 0);
        }

        return "index";
    }

    @GetMapping("/news")
    public String news(Model model) {
        try {
            List<Telegraph> news = newsService.getTelegraphList();
            model.addAttribute("news", news);
        } catch (Exception e) {
            log.error("Failed to fetch news", e);
            model.addAttribute("news", Collections.emptyList());
        }
        return "news";
    }

    @GetMapping("/reports")
    public String reports(@RequestParam(required = false) String code, Model model) {
        model.addAttribute("code", code);
        if (code != null && !code.isEmpty()) {
            try {
                List<ResearchReport> reports = reportService.getResearchReports(requireStockCode(code), 20, 1);
                model.addAttribute("reports", reports);
            } catch (Exception e) {
                log.error("Failed to fetch reports", e);
                model.addAttribute("reports", Collections.emptyList());
            }
        } else {
            model.addAttribute("reports", null);
        }
        return "reports";
    }

    @GetMapping("/hot")
    public String hot(Model model) {
        try {
            List<HotItem> weibo = hotTrendService.getWeiboHot(50);
            model.addAttribute("weibo", weibo);
        } catch (Exception e) {
            log.error("Failed to fetch weibo hot", e);
            model.addAttribute("weibo", Collections.emptyList());
        }

        try {
            List<HotItem> baidu = hotTrendService.getBaiduHot(50);
            model.addAttribute("baidu", baidu);
        } catch (Exception e) {
            log.error("Failed to fetch baidu hot", e);
            model.addAttribute("baidu", Collections.emptyList());
        }
        return "hot";
    }

    @GetMapping("/longhubang")
    public String longhubang(Model model) {
        try {
            List<LongHuBangItem> items = longHuBangService.getLongHuBangList();
            model.addAttribute("items", items);
        } catch (Exception e) {
            log.error("Failed to fetch longhubang", e);
            model.addAttribute("items", Collections.emptyList());
        }
        return "longhubang";
    }

    @GetMapping("/guba")
    public String guba(@RequestParam(required = false) String code, Model model) {
        model.addAttribute("code", code);
        if (code != null && !code.isEmpty()) {
            try {
                List<GubaPost> posts = gubaService.getPostList(requireStockCode(code), 30);
                model.addAttribute("posts", posts);
            } catch (Exception e) {
                log.error("Failed to fetch guba posts", e);
                model.addAttribute("posts", Collections.emptyList());
            }
        } else {
            model.addAttribute("posts", null);
        }
        return "guba";
    }

    @GetMapping("/guba-hot-comments")
    public String gubaHotComments(@RequestParam(required = false) String code, Model model) {
        model.addAttribute("code", code);
        if (code != null && !code.isEmpty()) {
            try {
                List<GubaComment> comments = gubaService.getTop3PostComments(requireStockCode(code));
                model.addAttribute("comments", comments);
            } catch (Exception e) {
                log.error("Failed to fetch guba hot comments for {}", code, e);
                model.addAttribute("comments", Collections.emptyList());
                model.addAttribute("error", "获取热帖评论失败，请稍后重试");
            }
        } else {
            model.addAttribute("comments", null);
        }
        return "guba-hot-comments";
    }

    @GetMapping("/rank")
    public String rank(Model model) {
        try {
            List<StockRankItem> items = stockRankService.getRankList(50);
            model.addAttribute("items", items);
        } catch (Exception e) {
            log.error("Failed to fetch rank", e);
            model.addAttribute("items", Collections.emptyList());
        }
        return "rank";
    }

    // ─── 股票行情功能 ─────────────────────────────────────────────────────────────

    @GetMapping("/quote")
    public String quote(@RequestParam(required = false) String code, Model model) {
        model.addAttribute("code", code);
        if (code != null && !code.isBlank()) {
            try {
                StockQuote quote = stockMarketWebService.getQuote(requireStockCode(code));
                model.addAttribute("quote", quote);
            } catch (Exception e) {
                log.error("Failed to fetch quote for {}", code, e);
                model.addAttribute("quote", null);
                model.addAttribute("error", "获取行情失败，请稍后重试");
            }
        }
        return "quote";
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String keyword, Model model) {
        model.addAttribute("keyword", keyword);
        if (keyword != null && !keyword.isBlank()) {
            try {
                List<StockQuote> results = stockMarketWebService.searchStock(keyword.trim());
                model.addAttribute("results", results);
            } catch (Exception e) {
                log.error("Failed to search stock for {}", keyword, e);
                model.addAttribute("results", Collections.emptyList());
                model.addAttribute("error", "搜索失败: " + e.getMessage());
            }
        } else {
            model.addAttribute("results", null);
        }
        return "quote";
    }

    @GetMapping("/kline")
    public String kline(@RequestParam(required = false) String code,
                        @RequestParam(required = false, defaultValue = "day") String period,
                        @RequestParam(required = false, defaultValue = "60") int days,
                        Model model) {
        model.addAttribute("code", code);
        model.addAttribute("period", period);
        model.addAttribute("days", days);
        if (code != null && !code.isBlank()) {
            try {
                int safeDays = StockCodeUtils.clamp(days, 1, 500);
                model.addAttribute("days", safeDays);
                List<KLineData> klines = stockMarketWebService.getKLineData(
                        requireStockCode(code), period, safeDays);
                model.addAttribute("klines", klines);
            } catch (Exception e) {
                log.error("Failed to fetch kline for {}", code, e);
                model.addAttribute("klines", Collections.emptyList());
                model.addAttribute("error", "获取K线失败，请稍后重试");
            }
        }
        return "kline";
    }

    @GetMapping("/technical")
    public String technical(@RequestParam(required = false) String code,
                             @RequestParam(required = false, defaultValue = "120") int days,
                             Model model) {
        model.addAttribute("code", code);
        model.addAttribute("days", days);
        if (code != null && !code.isBlank()) {
            try {
                int safeDays = StockCodeUtils.clamp(days, 1, 500);
                model.addAttribute("days", safeDays);
                TechnicalIndicators indicators = stockMarketWebService.getTechnicalIndicators(
                        requireStockCode(code), safeDays);
                model.addAttribute("indicators", indicators);
            } catch (Exception e) {
                log.error("Failed to fetch technical indicators for {}", code, e);
                model.addAttribute("indicators", null);
                model.addAttribute("error", "获取技术指标失败，请稍后重试");
            }
        }
        return "technical";
    }

    @GetMapping("/market-news")
    public String marketNews(@RequestParam(required = false) String keyword, Model model) {
        model.addAttribute("keyword", keyword);
        try {
            List<MarketNewsItem> news = marketNewsWebService.listHotNews(keyword, 30);
            model.addAttribute("news", news);
        } catch (Exception e) {
            log.error("Failed to fetch market news", e);
            model.addAttribute("news", Collections.emptyList());
        }
        try {
            List<MarketHotBoard> boards = marketNewsWebService.listHotBoards(20);
            model.addAttribute("boards", boards);
        } catch (Exception e) {
            log.error("Failed to fetch market hot boards", e);
            model.addAttribute("boards", Collections.emptyList());
        }
        return "market-news";
    }

    // ─── 财务数据功能 ─────────────────────────────────────────────────────────────

    @GetMapping("/financial")
    public String financial(@RequestParam(required = false) String code,
                            @RequestParam(required = false, defaultValue = "4") int count,
                            Model model) {
        model.addAttribute("code", code);
        model.addAttribute("count", count);
        if (code != null && !code.isBlank()) {
            try {
                int safeCount = StockCodeUtils.clamp(count, 1, 20);
                model.addAttribute("count", safeCount);
                var indicators = financialWebService.getFinancialIndicators(requireStockCode(code), safeCount);
                model.addAttribute("indicators", indicators);
            } catch (Exception e) {
                log.error("Failed to fetch financial indicators for {}", code, e);
                model.addAttribute("indicators", Collections.emptyList());
                model.addAttribute("error", "获取财务指标失败，请稍后重试");
            }
        }
        return "financial";
    }

    @GetMapping("/holdings")
    public String holdings(@RequestParam(required = false) String code,
                           @RequestParam(required = false, defaultValue = "4") int count,
                           Model model) {
        model.addAttribute("code", code);
        model.addAttribute("count", count);
        if (code != null && !code.isBlank()) {
            try {
                int safeCount = StockCodeUtils.clamp(count, 1, 20);
                model.addAttribute("count", safeCount);
                var data = financialWebService.getShareholderConcentration(requireStockCode(code), safeCount);
                model.addAttribute("data", data);
            } catch (Exception e) {
                log.error("Failed to fetch shareholder concentration for {}", code, e);
                model.addAttribute("data", Collections.emptyList());
                model.addAttribute("error", "获取股东集中度失败，请稍后重试");
            }
        }
        return "holdings";
    }

    private String requireStockCode(String code) {
        return StockCodeUtils.normalizeWithMarket(code);
    }
}
