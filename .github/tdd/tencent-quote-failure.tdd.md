# 腾讯报价异常透传 TDD 证据

日期：2026-07-10

## 来源与用户旅程

本次修复来源于跨 Agent Review：腾讯报价源把网络、HTTP 和超时异常转换为空列表，导致上层无法区分“合法无数据”和“数据源故障”。

用户旅程：作为行情编排服务，我需要收到腾讯报价源的真实访问异常，以便执行多源回退、累计健康失败并在达到阈值后冷却该能力；无效股票代码或合法空响应不能使整个数据源进入冷却。

## RED/GREEN 检查点

| 阶段 | 提交 | 证据 |
|---|---|---|
| RED | `e00eb0e` | 新测试因 `TencentDataSource` 缺少可注入的响应获取器而编译失败，无法隔离并验证网络异常语义。 |
| GREEN | `75fe367` | 网络异常包装为 `MarketDataAccessException`，非空非法响应报告解析失败，合法空响应继续返回空列表。 |

## 测试规格

| # | 保证 | 测试 | 类型 | 结果 |
|---|---|---|---|---|
| 1 | 网络超时抛出 `MarketDataAccessException`，保留 `Tencent/quote` 上下文和原始 cause | `TencentDataSourceFailureTest.wrapsNetworkTimeoutAsMarketDataAccessException` | 单元测试 | PASS |
| 2 | 非空且不包含腾讯响应信封的内容被识别为非法响应 | `TencentDataSourceFailureTest.rejectsMalformedNonEmptyResponse` | 单元测试 | PASS |
| 3 | 腾讯的无匹配响应 `v_pv_none_match="1"` 仍返回空列表 | `TencentDataSourceFailureTest.keepsNoMatchResponseAsEmptyResult` | 单元测试 | PASS |
| 4 | 空响应仍返回空列表 | `TencentDataSourceFailureTest.keepsBlankResponseAsEmptyResult` | 单元测试 | PASS |
| 5 | 空股票代码列表不发起远程请求 | `TencentDataSourceFailureTest.skipsRequestForEmptyCodes` | 单元测试 | PASS |

## 验证

```bash
mvn -pl stock-crawler -Dtest=TencentDataSourceFailureTest test
mvn -pl stock-crawler test
mvn -pl stock-crawler -am install
mvn test -pl agent-invest-data,agent-invest-web -am
```

RED 阶段的定向测试以预期的构造器缺失编译失败。GREEN 后定向测试 5 个通过，爬虫模块全量测试通过；安装后的本地 Maven JAR 与构建产物 SHA-256 一致，AgentInvest 相关 Reactor 测试通过。

JaCoCo 0.8.12 针对 `TencentDataSourceFailureTest` 和 `TencentDataSourceParseTest` 的报告中，`TencentDataSource` 行覆盖为 83 / 94（88.30%），指令覆盖为 373 / 414（90.10%）。

## 已知未覆盖项

- 未人为制造线上腾讯接口故障；故障回退依赖确定性的单元测试和 AgentInvest 后端测试验证。
- 未执行浏览器测试；本次改动不涉及前端，且用户明确要求不打开浏览器。

RED 与 GREEN 证据分别保存在上述两个独立提交中，不需要依赖工作区日志还原。
