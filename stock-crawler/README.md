# Stock Crawler

> A股市场数据爬虫 — 实时行情、K线技术分析、财经快讯、研报、舆情热点、龙虎榜、股吧、个股人气榜、市场新闻

## 项目简介

Stock Crawler 是一个 Java 实现的 A 股数据爬虫库，聚合腾讯财经、百度股市通、新浪财经、东方财富、财联社、微博等主流财经平台的公开数据。核心库基于 Maven 构建，**零 Spring 依赖**，可作为独立工具使用，也可轻松集成到任意 Java 项目中。

配套的 `stock-crawler-web` 模块基于 Spring Boot + Thymeleaf 提供 Web 界面，开箱即用。

## 功能特性

| 模块 | 数据源 | 功能描述 |
|------|--------|----------|
| 实时行情 | 腾讯财经优先，新浪兜底 | 股票实时报价（OHLC、成交量、成交额）、PE/PB/总市值/换手率 |
| K线数据 | 百度股市通 + 新浪，东方财富末位兜底 | 日K/周K/月K历史数据，含涨跌幅、换手率 |
| 技术指标 | ta4j | MA、MACD、RSI、KDJ、布林带等常用技术指标计算 |
| 股票搜索 | 新浪财经 | 按关键字搜索股票代码和名称 |
| 财经快讯 | 财联社 | 获取实时财经快讯 |
| 市场新闻 | 东方财富 7x24 + 百度股市 + 财联社兜底 | 多源市场新闻聚合、去重排序、热门板块 |
| 研报服务 | 东方财富 | 按股票代码查询券商研报及PDF链接 |
| 舆情热点 | 微博/百度 | 聚合多平台热搜榜单 |
| 龙虎榜 | 东方财富 | 获取每日龙虎榜数据及席位明细 |
| 股吧服务 | 东方财富 | 获取股吧帖子和评论，热度前N帖子评论聚合 |
| 个股人气榜 | 东方财富 | 获取个股人气排行榜 |

## 技术栈

| 组件 | 版本 | 用途 |
|------|------|------|
| Java | 21 (LTS) | 运行环境 |
| Maven | 3.8+ | 项目构建 |
| OkHttp | 4.12.0 | HTTP 客户端 |
| Jsoup | 1.17.2 | HTML 解析 |
| Jackson | 2.17.0 | JSON 解析 |
| ta4j | 0.17 | 技术指标计算 |
| JUnit 5 | 5.10.2 | 单元 & 集成测试 |
| SLF4J + Logback | 2.0 / 1.5 | 日志框架 |

Web 模块额外依赖：

| 组件 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.2.4 | Web 框架 |
| Thymeleaf | — | 模板引擎 |

## 项目结构

```
stock-crawler/                          # 核心爬虫库（零 Spring 依赖）
├── pom.xml
└── src/
    ├── main/java/com/stock/crawler/
    │   ├── model/                      # 数据模型
    │   │   ├── StockQuote.java         # 实时行情
    │   │   ├── KLineData.java          # K线数据
    │   │   ├── TechnicalIndicators.java # 技术指标
    │   │   ├── Telegraph.java          # 财经快讯
    │   │   ├── MarketNewsItem.java     # 市场新闻
    │   │   ├── MarketHotBoard.java     # 热门板块
    │   │   ├── ResearchReport.java     # 研报
    │   │   ├── HotItem.java            # 热点条目
    │   │   ├── HotTrendResult.java     # 热点聚合结果
    │   │   ├── LongHuBangItem.java     # 龙虎榜条目
    │   │   ├── LongHuBangDetail.java   # 龙虎榜席位明细
    │   │   ├── GubaPost.java           # 股吧帖子
    │   │   ├── GubaComment.java        # 股吧评论
    │   │   └── StockRankItem.java      # 个股人气榜
    │   ├── datasource/                 # 数据源（多源容灾）
    │   │   ├── MarketDataSource.java   # 数据源接口
    │   │   ├── TencentDataSource.java  # 腾讯财经（实时行情、估值、市值）
    │   │   ├── BaiduKLineDataSource.java # 百度股市通（日K线）
    │   │   ├── SinaDataSource.java     # 新浪财经（K线兜底、搜索）
    │   │   └── EastMoneyDataSource.java # 东方财富（末位K线兜底、独有数据补充）
    │   ├── service/                    # 业务服务层
    │   │   ├── StockMarketService.java # 行情聚合（含缓存、多源切换）
    │   │   ├── TechnicalIndicatorService.java # 技术指标计算
    │   │   ├── MarketNewsService.java  # 市场新闻（多源聚合、去重）
    │   │   ├── NewsService.java        # 财经快讯（含30s缓存）
    │   │   ├── ResearchReportService.java # 研报查询与PDF链接
    │   │   ├── HotTrendService.java    # 舆情热点（含5min缓存）
    │   │   ├── LongHuBangService.java  # 龙虎榜
    │   │   ├── GubaService.java        # 股吧帖子与评论
    │   │   └── StockRankService.java   # 个股人气榜
    │   ├── fetcher/                    # 平台热搜抓取器
    │   │   ├── Fetcher.java            # 抓取器接口
    │   │   ├── WeiboFetcher.java       # 微博热搜
    │   │   └── BaiduFetcher.java       # 百度热搜
    │   ├── util/
    │   │   └── HttpUtils.java          # HTTP 工具类（自动重试、东财统一限流入口）
    │   └── Demo.java                   # 研报交互式演示程序
    └── test/java/com/stock/crawler/
        ├── service/                    # 单元测试
        └── fetcher/                    # 集成测试（需网络）

stock-crawler-web/                      # Spring Boot Web 界面
├── pom.xml
└── src/
    └── main/
        ├── java/com/stock/web/
        │   └── controller/
        │       └── HomeController.java # 所有页面路由
        └── resources/
            ├── application.properties  # 配置（端口8080）
            └── templates/              # Thymeleaf 模板
                ├── layout.html         # 基础布局
                ├── index.html          # 首页仪表盘
                ├── quote.html          # 实时行情
                ├── kline.html          # K线图表
                ├── technical.html      # 技术指标
                ├── news.html           # 财经快讯
                ├── market-news.html    # 市场新闻
                ├── reports.html        # 研报
                ├── hot.html            # 热搜
                ├── longhubang.html     # 龙虎榜
                ├── guba.html           # 股吧帖子
                ├── guba-hot-comments.html # 热帖评论
                └── rank.html           # 人气榜
```

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.8+

### 编译项目

```bash
# 在仓库根目录编译并运行默认单元测试
mvn clean test

# 只打包 Web 模块及其依赖模块
mvn -pl stock-crawler-web -am clean package
```

### 启动 Web 服务

```bash
# 在仓库根目录启动 Web 模块
mvn -pl stock-crawler-web -am spring-boot:run
# 访问 http://localhost:8080
```

Web 界面提供以下页面：

| 路径 | 功能 |
|------|------|
| `/` | 首页仪表盘 |
| `/quote?code=sh600519` | 实时行情（含 PE/PB/市值/换手率） |
| `/kline?code=sh600519&period=day&days=60` | K线数据 |
| `/technical?code=sh600519&days=120` | 技术指标分析 |
| `/search?keyword=茅台` | 股票搜索 |
| `/news` | 财经快讯 |
| `/market-news` | 市场新闻 + 热门板块 |
| `/reports?code=600519` | 研报查询 |
| `/hot` | 微博 + 百度热搜 |
| `/longhubang` | 龙虎榜 |
| `/guba?code=600519` | 股吧帖子 |
| `/guba-hot-comments?code=600519` | 热帖评论（TOP3） |
| `/rank` | 个股人气榜 |

Web JSON API 同时提供 `/api/data/...` 与 `/api/capabilities/...` 两组入口，便于业务系统直接复用：

| 路径 | 功能 |
|------|------|
| `/api/data/quote/{code}` | 实时行情 |
| `/api/data/basic/{code}` | 个股基础资料，东财失败时降级腾讯行情字段 |
| `/api/data/themes/{code}` | 行业/概念/地域归因 |
| `/api/data/fund/daily/{code}?limit=20` | 日级资金流 |
| `/api/data/fund/minute/{code}` | 分钟级资金流 |
| `/api/data/company-news/{code}?limit=10` | 个股新闻 |
| `/api/data/flash?limit=20` | 7x24 市场快讯 |
| `/api/data/ann/{code}?limit=10` | 巨潮公告 |

### 运行测试

```bash
# 仅运行单元测试（默认，不需要网络）
mvn test

# 运行全部测试（含需要网络的集成测试）
mvn -pl stock-crawler test -Dintegration=true
```

### 运行演示程序

演示程序会依次展示平安银行、贵州茅台、五粮液的最新研报，并支持交互式输入股票代码查询。

```bash
mvn -pl stock-crawler exec:java -Dexec.mainClass="com.stock.crawler.Demo"
```

## API 使用示例

### 1. 实时行情

```java
StockMarketService marketService = new StockMarketService();

// 获取实时行情（优先腾讯财经，腾讯已包含PE/PB/市值/换手率）
StockQuote quote = marketService.getQuote("sh600519");

System.out.println(quote.getName());           // 贵州茅台
System.out.println(quote.getPrice());          // 最新价
System.out.println(quote.getOpen());           // 今开
System.out.println(quote.getPreClose());       // 昨收
System.out.println(quote.getHigh());           // 最高
System.out.println(quote.getLow());            // 最低
System.out.println(quote.getVolume());         // 成交量（股）
System.out.println(quote.getAmount());         // 成交额
System.out.println(quote.getTurnoverRate());   // 换手率
System.out.println(quote.getPe());             // 市盈率
System.out.println(quote.getPb());             // 市净率
System.out.println(quote.getMarketCap());      // 总市值
System.out.println(quote.getChange());         // 涨跌额
System.out.println(quote.getChangePercent());  // 涨跌幅

// 支持多种代码格式
marketService.getQuote("600519");       // 纯数字
marketService.getQuote("sh600519");     // 带市场前缀
marketService.getQuote("600519.SH");    // 后缀格式
```

### 2. K线数据

```java
StockMarketService marketService = new StockMarketService();

// 获取日K线（最近60个交易日，优先百度股市通）
List<KLineData> klines = marketService.getKLineData("sh600519", "day", 60);

// 获取周K线
List<KLineData> weekKlines = marketService.getKLineData("sh600519", "week", 52);

// 获取月K线
List<KLineData> monthKlines = marketService.getKLineData("sh600519", "month", 24);

for (KLineData k : klines) {
    System.out.printf("%s O:%s H:%s L:%s C:%s 涨跌幅:%s%% 换手率:%s%%%n",
        k.getDate(), k.getOpen(), k.getHigh(), k.getLow(), k.getClose(),
        k.getChangePercent(), k.getTurnoverRate());
}
```

### 3. 技术指标

```java
StockMarketService marketService = new StockMarketService();

// 获取技术指标分析（基于日K线计算）
TechnicalIndicators ti = marketService.getTechnicalIndicators("sh600519", 120);

System.out.println("MA5: " + ti.getMa5());
System.out.println("MA20: " + ti.getMa20());
System.out.println("MACD: " + ti.getMacd());
System.out.println("RSI6: " + ti.getRsi6());
System.out.println("KDJ K: " + ti.getKdjK());
System.out.println("布林上轨: " + ti.getBollingerUpper());
System.out.println("趋势信号: " + ti.getTrendSignal());
System.out.println("MACD信号: " + ti.getMacdSignal());
```

### 4. 股票搜索

```java
StockMarketService marketService = new StockMarketService();

// 按关键字搜索股票
List<StockQuote> results = marketService.searchStock("茅台");
for (StockQuote q : results) {
    System.out.println(q.getCode() + " " + q.getName());
}
```

### 5. 市场新闻

```java
MarketNewsService newsService = new MarketNewsService();

// 获取市场新闻（财联社 + 百度股市，自动去重排序）
List<MarketNewsItem> news = newsService.getMarketNews();

for (MarketNewsItem item : news) {
    System.out.println(item.getTime() + " [" + item.getSource() + "] " + item.getTitle());
}

// 获取热门板块
List<MarketHotBoard> boards = newsService.getHotBoards();
for (MarketHotBoard board : boards) {
    System.out.println("=== " + board.getBoardName() + " (" + board.getSource() + ") ===");
    for (MarketHotItem hotItem : board.getItems()) {
        System.out.printf("  %d. %s%n", hotItem.getRank(), hotItem.getTitle());
    }
}
```

### 6. 财经快讯

```java
NewsService newsService = new NewsService();

// 获取最新快讯列表（带30秒缓存）
List<Telegraph> news = newsService.getTelegraphList();

for (Telegraph t : news) {
    System.out.println(t.getTime() + " - " + t.getContent());
}

// 获取最新一条快讯
Telegraph latest = newsService.getLatestTelegraph();
```

### 7. 研报查询

```java
ResearchReportService reportService = new ResearchReportService();

// 按股票代码查询研报
List<ResearchReport> reports = reportService.getResearchReports("000001");

for (ResearchReport r : reports) {
    System.out.println(r.getTitle());
    System.out.println("  券商: " + r.getOrgSName());
    System.out.println("  评级: " + r.getEmRatingName());
    System.out.println("  研究员: " + r.getResearcher());
}

// 获取研报 PDF 链接
String pdfUrl = reportService.getReportPdfUrl(report.getInfoCode());
```

### 8. 舆情热点

```java
HotTrendService hotTrendService = new HotTrendService();

// 获取聚合热点（微博+百度）
List<HotTrendResult> results = hotTrendService.getAllHotTrends();

for (HotTrendResult result : results) {
    System.out.println("=== " + result.getPlatform() + " ===");
    for (HotItem item : result.getItems()) {
        System.out.printf("%d. %s (热度: %d)%n",
            item.getRank(), item.getTitle(), item.getHotScore());
    }
}

// 获取指定平台热搜
HotTrendResult weiboHot = hotTrendService.getHotTrend("weibo");
HotTrendResult baiduHot = hotTrendService.getHotTrend("baidu");
```

### 9. 龙虎榜

```java
LongHuBangService longHuBangService = new LongHuBangService();

// 获取今日龙虎榜
LongHuBangResult result = longHuBangService.getLongHuBangList();

for (LongHuBangItem item : result.getItems()) {
    System.out.printf("%s (%s) 涨幅: %.2f%% 净买入: %.2f万%n",
        item.getName(), item.getCode(),
        item.getChangePercent(), item.getNetBuyAmt() / 10000);
}

// 获取指定日期龙虎榜
LongHuBangResult specificDate = longHuBangService.getLongHuBangList(50, 1, "2026-03-25");

// 获取个股龙虎榜详情
List<LongHuBangDetail> details = longHuBangService.getStockDetail("000001", "2026-03-25");
```

### 10. 股吧帖子

```java
GubaService gubaService = new GubaService();

// 获取帖子列表（股票代码, 每页数量, 页码）
List<GubaPost> posts = gubaService.getPostList("000001", 20, 1);

for (GubaPost post : posts) {
    System.out.printf("%s (阅读: %d, 评论: %d)%n",
        post.getTitle(), post.getReadCount(), post.getCommentCount());
}

// 获取帖子详情（股票代码, 帖子ID）
GubaPost detail = gubaService.getPostDetail("000001", "1684774522");
System.out.println("内容: " + detail.getContent());

// 获取帖子评论（帖子ID, 每页数量, 页码）
List<GubaComment> comments = gubaService.getComments("1684774522", 10, 1);

for (GubaComment c : comments) {
    System.out.printf("%s: %s (点赞: %d)%n",
        c.getAuthor(), c.getContent(), c.getLikeCount());
}

// 获取热度最高的前3个帖子的评论（按阅读量+评论数+点赞数排序）
List<GubaComment> hotComments = gubaService.getTop3PostComments("000001");

// 或自定义：取前5个帖子，每个帖子取10条评论
List<GubaComment> custom = gubaService.getTopPostComments("000001", 5, 10);
```

### 11. 个股人气榜

```java
StockRankService stockRankService = new StockRankService();

// 获取人气榜 Top 50
List<StockRankItem> items = stockRankService.getRankList(50);

for (StockRankItem item : items) {
    System.out.printf("%d. %s (%s) 涨幅: %.2f%% %s%n",
        item.getRank(),
        item.getStockName(),
        item.getStockCode(),
        item.getChangePercent(),
        item.getRankChangeDesc());
}
```

## 数据源

| 外部来源 | 数据 | 使用方 |
|----------|------|--------|
| 腾讯财经 (`qt.gtimg.cn`) | 实时行情、PE/PB/市值/换手率 | TencentDataSource |
| 百度股市通 (`finance.pae.baidu.com`) | 日K线 | BaiduKLineDataSource |
| 新浪财经 (`hq.sinajs.cn`) | 实时行情兜底 | SinaDataSource |
| 新浪搜索 (`suggest3.sinajs.cn`) | 股票搜索 | SinaDataSource |
| 东方财富 K线 (`push2his.eastmoney.com`) | K线末位兜底 | EastMoneyDataSource |
| 东方财富估值 (`push2.eastmoney.com`) | 显式估值补充工具，默认不进入实时行情热路径 | EastMoneyDataSource |
| 东方财富研报 (`reportapi.eastmoney.com`) | 券商研报 | ResearchReportService |
| 东方财富数据中心 | 龙虎榜 | LongHuBangService |
| 东方财富人气 (`emappdata.eastmoney.com`) | 个股人气排行 | StockRankService |
| 东方财富股吧 (`guba.eastmoney.com`) | 股吧帖子与评论 | GubaService |
| 东方财富 7x24 (`np-weblist.eastmoney.com`) | 市场快讯 | MarketNewsService, StockIntelligenceService |
| 财联社 (`cls.cn`) | 财经快讯、市场新闻兜底 | NewsService, MarketNewsService |
| 百度股市 (`finance.pae.baidu.com`) | 市场新闻、热门板块 | MarketNewsService |
| 微博 (`weibo.com`) | 热搜榜单 | WeiboFetcher |
| 百度 (`top.baidu.com`) | 热搜榜单 | BaiduFetcher |

### 数据源优先级与东财限流

行情与 K 线默认按低封禁风险优先：腾讯财经（实时行情/估值）→ 百度股市通（日 K）→ 新浪财经（K 线兜底/搜索）→ 东方财富（末位兜底与独有数据）。东方财富仅用于其独有或明确请求的数据，例如龙虎榜、研报、股吧、人气榜、财务指标、资金流、概念归因和公告新闻补充，不再阻塞实时行情热路径。

所有 `eastmoney.com` / `dfcfw.com` 请求都通过 `HttpUtils.getEastMoney(...)` 或 `HttpUtils` 内部识别后统一串行限流，默认最小间隔为 `1200ms + 100~500ms` 抖动，并带网络异常重试。批量任务可通过 JVM 参数调大间隔：

```bash
-Dstockcrawler.eastmoney.minIntervalMs=2000
```

## 缓存机制

多个服务内置基于时间戳的轻量缓存，无需额外依赖：

| 服务 | 缓存时长 | 说明 |
|------|----------|------|
| StockMarketService（行情） | 5 秒 | 行情变化频繁，短缓存保证时效性 |
| StockMarketService（K线） | 5 分钟 | K线数据日内变化较少 |
| NewsService | 30 秒 | 财联社快讯刷新频率较高 |
| HotTrendService | 5 分钟 | 热搜榜单变化较慢 |
| MarketNewsService | 90 秒 | 多源新闻聚合，适度缓存 |
| LongHuBangService | 5 分钟 | 龙虎榜日内不变 |

## 注意事项

1. **仅供学习研究使用**，请勿用于商业用途
2. 请遵守目标网站的 robots.txt 和服务条款
3. 建议在循环调用时添加适当的请求间隔，避免对目标服务器造成压力
4. 数据接口依赖第三方网站，可能随对方更新而失效，请关注项目动态

## License

[MIT License](../LICENSE)
