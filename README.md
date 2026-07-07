# A Stock Crawler

A Stock Crawler 是一个 Java 21 + Maven 多模块项目，用来聚合公开的 A 股市场数据，并提供一个 Spring Boot + Thymeleaf 的 Web 演示入口。

仓库里真正可复用的是 `stock-crawler` 核心库。`stock-crawler-web` 主要用于页面演示和接口调试，业务系统可以只依赖核心库，不需要引入 Web 模块。

> 数据来自第三方公开页面或公开接口，接口结构和访问策略可能变化。本项目适合学习、研究、内部工具和二次开发，不构成投资建议。

## 模块说明

| 模块 | 作用 | 适用场景 |
| --- | --- | --- |
| `stock-crawler` | 核心抓取库，零 Spring 依赖 | 被 Java 项目直接依赖，或作为命令行/批处理工具使用 |
| `stock-crawler-web` | Spring Boot Web 演示项目 | 本地查看行情、新闻、研报、股吧、龙虎榜等数据 |

根目录的 `pom.xml` 是 Maven 聚合工程，统一管理 Java 版本、依赖版本和测试配置。

## 覆盖的数据

`stock-crawler` 当前聚合了腾讯财经、百度股市通、新浪财经、东方财富、财联社、微博、百度热搜等公开数据源。

| 能力 | 主要数据源 | 说明 |
| --- | --- | --- |
| 实时行情 | 腾讯财经，新浪兜底 | 股票报价、OHLC、成交量、成交额、PE、PB、市值、换手率 |
| K 线数据 | 百度股市通，新浪和东方财富兜底 | 日 K、周 K、月 K 历史数据 |
| 技术指标 | ta4j | MA、MACD、RSI、KDJ、布林带等指标计算 |
| 财经快讯 | 财联社 | 实时财经快讯 |
| 市场新闻 | 东方财富、百度股市、财联社 | 7x24 快讯、个股新闻、热门板块 |
| 研报 | 东方财富 | 按股票代码查询券商研报和 PDF 链接 |
| 舆情热点 | 微博、百度 | 多平台热搜榜单 |
| 龙虎榜 | 东方财富 | 龙虎榜列表和席位明细 |
| 股吧 | 东方财富 | 股吧帖子、评论和热帖评论聚合 |
| 个股人气榜 | 东方财富 | 个股人气排行 |

更完整的 API 示例在 [stock-crawler/README.md](stock-crawler/README.md)。

## 环境要求

- JDK 21+
- Maven 3.8+

## 构建和测试

在仓库根目录执行：

```bash
mvn clean test
```

默认测试会排除带 `@Tag("integration")` 的网络集成测试，适合本地开发和 CI 的快速检查。

需要跑真实网络接口时，执行：

```bash
mvn -pl stock-crawler test -Dintegration=true
```

只打包 Web 演示项目及其依赖模块：

```bash
mvn -pl stock-crawler-web -am clean package
```

## 启动 Web 演示

```bash
mvn -pl stock-crawler-web -am spring-boot:run
```

默认端口是 `8080`，启动后访问：

```text
http://localhost:8080
```

开发时可以开启 `dev` profile，关闭 Thymeleaf 缓存并打开更详细的 Web 日志：

```bash
mvn -pl stock-crawler-web -am spring-boot:run -Dspring-boot.run.profiles=dev
```

常用页面：

| 路径 | 说明 |
| --- | --- |
| `/` | 首页仪表盘 |
| `/quote?code=sh600519` | 实时行情 |
| `/kline?code=sh600519&period=day&days=60` | K 线数据 |
| `/technical?code=sh600519&days=120` | 技术指标 |
| `/news` | 财经快讯 |
| `/market-news` | 市场新闻和热门板块 |
| `/reports?code=600519` | 个股研报 |
| `/hot` | 微博和百度热搜 |
| `/longhubang` | 龙虎榜 |
| `/guba?code=600519` | 股吧帖子 |
| `/rank` | 个股人气榜 |

## 作为依赖使用

本地开发时，先把父 POM 和核心模块安装到本地 Maven 仓库：

```bash
mvn install -DskipTests
```

如果只想安装核心库，也要先安装根目录父 POM：

```bash
mvn install -N -DskipTests
mvn -pl stock-crawler install -DskipTests
```

外部项目引用：

```xml
<dependency>
    <groupId>com.stock</groupId>
    <artifactId>stock-crawler</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

`stock-crawler` 的 POM 继承了根项目 `com.stock:a-stock-crawler:pom:1.0-SNAPSHOT`。如果把快照发布到私服或给其他项目使用，需要一起发布父 POM，否则外部项目会在解析依赖描述时找不到父 POM。

简单示例：

```java
import com.stock.crawler.model.StockQuote;
import com.stock.crawler.service.StockMarketService;

public class QuoteExample {
    public static void main(String[] args) throws Exception {
        StockMarketService service = new StockMarketService();
        StockQuote quote = service.getQuote("600519");
        System.out.println(quote.getName() + " " + quote.getPrice());
    }
}
```

股票代码支持多种常见写法，例如 `600519`、`sh600519`、`600519.SH`、`000001.sz`、`bj430047`。核心库会在服务入口统一处理这些格式。

## JSON API

`stock-crawler-web` 提供页面，也提供 JSON API，便于前端或其他服务联调：

| 路径 | 说明 |
| --- | --- |
| `/api/data/quote/{code}` | 实时行情 |
| `/api/data/basic/{code}` | 个股基础资料 |
| `/api/data/themes/{code}` | 行业、概念、地域归因 |
| `/api/data/fund/daily/{code}?limit=20` | 日级资金流 |
| `/api/data/fund/minute/{code}` | 分钟级资金流 |
| `/api/data/company-news/{code}?limit=10` | 个股新闻 |
| `/api/data/flash?limit=20` | 7x24 市场快讯 |
| `/api/data/ann/{code}?limit=10` | 巨潮公告 |

API 返回统一的数据包装结构。抓取失败、空数据或第三方接口异常时，会返回失败结果，而不是把空数据包装成成功。

## CI

仓库内置 GitHub Actions：

- `push` 和 `pull_request`：运行 `mvn test`。
- `workflow_dispatch` 和定时任务：运行 `mvn -pl stock-crawler test -Dintegration=true`，覆盖需要网络的集成测试。

网络集成测试依赖第三方站点可用性。遇到第三方接口限流、页面结构调整或临时不可用时，需要先区分是项目解析逻辑问题，还是外部数据源变化。

## 最近的工程化整理

当前根工程已经补齐了几项基础能力：

- 根聚合 POM 统一管理两个模块。
- 核心库不再把 Logback 作为编译期传递依赖。
- 股票代码格式统一由核心工具处理。
- 字节请求和 GBK 响应路径补上重试逻辑。
- Web 演示项目增加安全 URL 校验、统一异常返回和模板空值容错。
- CI 区分默认单元测试和需要网络的集成测试。

这些改动主要服务于两个目标：核心库方便被 `agent-invest` 这类业务项目引用，Web 模块保留为轻量演示入口。

## 许可证

本项目使用 [MIT License](LICENSE)。
