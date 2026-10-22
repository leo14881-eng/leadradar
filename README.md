# LeadRadar

替胡志明的中资小商家盯 Facebook 群组和本地站点里的「求购 / 求租 / 找服务」帖，
推送带中文摘要和越南语建议回复的线索，帮老板多接单。

## MVP 范围

| 做 | 不做（后续版本） |
| --- | --- |
| 线索雷达：采集 → 规则粗筛 → LLM 精判 → Telegram 推送 | 接管商家主页 / Zalo OA 自动客服 |
| 建议回复：老板确认后自己发 | 替老板对外发消息 |
| 每日简报 | 网页后台（商家配置先写在 yml） |

## 架构

```
PostSource（manual / 后续：facebook-group、chotot …）
   → 去重（seen_posts 表，重启不丢）
   → RuleFilter（去声调关键词 + 区域 + 排除词）
   → LeadJudge（none：关键词命中即推；claude / openai：判断是否潜在客户、中文摘要、越南语建议回复、打分）
   → 入库（leads 表）→ Notifier（Telegram）
DailyBriefJob：每天 21:00（胡志明时间）推送今日线索 / 待跟进 / 本周成交
```

模型调用失败或输出无法解析时，自动退化为"关键词命中即推送原文"，宁可多推不漏单。

## 运行

```bash
mvn spring-boot:run
```

| 环境变量 | 说明 | 默认 |
| --- | --- | --- |
| `LLM_PROVIDER` | `none` / `claude` / `openai` | `none` |
| `LLM_API_KEY` | 对应厂商的 API key | 空 |
| `LLM_MODEL` | 模型名，留空用默认（`claude-haiku-5-5` / `gpt-5-mini`） | 空 |
| `LLM_BASE_URL` | 兼容 OpenAI 协议的其他服务地址 | 官方地址 |
| `TELEGRAM_BOT_TOKEN` | 未配置时推送只打日志 | 空 |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | 换 PostgreSQL 时设置 | 本地 H2 文件 `./data/leadradar` |

商家配置在 `application.yml` 的 `leadradar.merchants`，真实商家写到 `application-local.yml`（不进仓库），用 `--spring.profiles.active=local` 启动。

## 接口

```bash
# 塞一条帖子（验证期由人工或 Claude 浏览后导入）
curl -X POST localhost:8088/api/posts -H 'Content-Type: application/json' \
  -d '{"source":"manual","url":"https://example.com/p/1","text":"Ai biết quán món Trung giao tới Sunrise City không?"}'

# 查线索
curl 'localhost:8088/api/leads?merchantId=demo-chinese-restaurant-q7&status=NEW'

# 记录跟进结果：NEW / CONTACTED / WON / LOST / IRRELEVANT
curl -X PATCH localhost:8088/api/leads/1 -H 'Content-Type: application/json' \
  -d '{"status":"WON","note":"4人火锅，消费120万盾"}'
```

## 测试

```bash
mvn test   # 单元测试 + Spring 集成测试（内存 H2，不调外部接口）
```

## 约定

- 数据源只读、低频，优先公开站点与官方接口，不做批量爬虫
- 帖子作者信息只用于展示，不入库
- 商家真实配置、`data/` 目录不进仓库
- 老板对外发消息由本人确认后自己发，系统不代发

## 下一步

1. 第一个自动数据源（优先公开站点，如 Chotot）
2. Telegram 里直接回复 `#线索号 成交` 更新状态，不用调接口
3. 换 PostgreSQL 部署到 VPS
