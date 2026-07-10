# K 线聚合与回退 TDD 证据

日期：2026-07-10

## RED/GREEN 检查点

| 阶段 | 提交 | 证据 |
|---|---|---|
| RED | `6b83689` | 聚合器、年周期与多源日 K 回退测试因目标 API 不存在而失败。 |
| GREEN | `29027de` | `KLineAggregator`、能力声明、非日周期日 K 聚合与多源回退实现通过。 |
| RED | `e15b8b2` | 数据访问异常测试因 `MarketDataAccessException` 不存在而失败。 |
| GREEN | `ff173f3` | 网络、超时与解析错误统一包装，合法无数据仍返回空列表。 |
| COVERAGE | `2e11946` | 补齐 ISO 跨年周、日 K 截取、无效记录与零前收边界测试。 |

## 验证

```bash
mvn -pl stock-crawler test
mvn -pl stock-crawler -am install
```

完整模块测试 63 个通过，其中 `KLineAggregatorTest` 6 个通过。

JaCoCo 0.8.13 报告中，`KLineAggregator` 行覆盖为 102 / 104（98.08%），指令覆盖为 396 / 400（99.00%）。

安装后的 `~/.m2/repository/com/stock/stock-crawler/1.0-SNAPSHOT/stock-crawler-1.0-SNAPSHOT.jar` 已确认包含 `KLineAggregator` 与 `MarketDataAccessException`。
