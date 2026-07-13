# 市场广度聚合接口：TDD 证据

## 用户旅程

- 作为市场情绪调用方，我希望一次刷新只请求涨跌分布和全市场总数，而不是抓取 56 页个股行情。
- 作为评分调用方，接口字段异常或日期过期时必须收到失败结果，不能基于残缺数据生成分数。

## RED / GREEN

| 阶段 | 命令 | 结果 |
|---|---|---|
| RED | `mvn -pl stock-crawler -Dtest=MarketBreadthServiceTest test` | 失败：旧实现仍读取分页 `data.diff`，9 个聚合契约用例全部失败。 |
| GREEN | 同一命令 | 通过：9 个测试。 |
| 全量回归 | `mvn -pl stock-crawler test` | 通过：125 个测试。 |
| 本地安装 | `mvn -pl stock-crawler -am install` | 通过：125 个测试，并安装 `stock-crawler:1.0-SNAPSHOT`。 |
| 真实契约 | `mvn -pl stock-crawler -Dintegration=true -Dtest=MarketBreadthServiceIntegrationTest test` | 聚合主链路在 10 秒内成功；涨跌停池临时返回 `rc=102`，相等性校验按外部接口不可用标记为跳过。 |

## 测试保证

| 保证 | 测试 |
|---|---|
| 单次刷新只请求涨跌分布和 `clist?pz=1` 两个接口 | `fetchesOnlyDistributionAndTotalThenBuildsVersionTwoSnapshot` |
| 正确统计涨、跌、平盘、停牌、涨跌停和保守的极端分布 | `fetchesOnlyDistributionAndTotalThenBuildsVersionTwoSnapshot` |
| 缺少 `fenbu`、非法桶、负数、重复桶、非整数和过期 `qdate` 均失败 | `rejectsInvalidDistributionResponses` |
| 总数必须在 1000～10000，且不能小于活跃数 | `rejectsTotalOutsideExpectedRange`、`rejectsActiveCountGreaterThanTotalCount` |
| `averageChangePercent` 为 `null`，不使用区间中点伪造平均涨跌幅 | `fetchesOnlyDistributionAndTotalThenBuildsVersionTwoSnapshot` |
| 真实环境中总数合理，涨跌停池可用时 `±11` 与池总数严格一致 | `MarketBreadthServiceIntegrationTest` |

## 实现备注

- 分布来源为 `push2ex.eastmoney.com/getTopicZDFenBu`。
- `push2.eastmoney.com` 在真实测试中出现提前断流，总数请求改用同接口的 `push2delay.eastmoney.com` 节点。
- 运行时不请求涨跌停池，也不回退到分页抓取；失败交由 AgentInvest 保留旧快照。
