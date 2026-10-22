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
PostSource（manual / facebook-group / chotot …）
   → 去重 → RuleFilter（去声调关键词 + 区域 + 排除词）
   → LeadJudge（Claude：是否潜在客户、中文摘要、越南语回复）
   → Notifier（Telegram）
```

## 运行

```bash
mvn spring-boot:run
# 手动塞一条帖子测试整条链路（未配 TELEGRAM_BOT_TOKEN 时只打日志）
curl -X POST localhost:8088/api/posts -H 'Content-Type: application/json' \
  -d '{"source":"manual","url":"https://example.com/p/1","text":"Ai biết quán món Trung giao tới Sunrise City không?"}'
```

## 约定

- 数据源只读、低频，优先公开站点与官方接口，不做批量爬虫
- 帖子作者信息只用于展示，不长期存储
- 商家真实配置放 `application-local.yml`，不进仓库

## 下一步

1. `ClaudeLeadJudge`：接 Claude API 做精判和回复生成
2. PostgreSQL 持久化去重与线索记录（记录每条线索是否成交）
3. 每日简报定时任务
4. 第一个自动数据源
