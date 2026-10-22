package com.leadradar.lead;

/** 线索跟进状态。验证期最关键的数据是 WON：每月帮商家多成交了几单。 */
public enum LeadStatus {
    /** 刚推送，老板还没处理 */
    NEW,
    /** 老板已回复/联系 */
    CONTACTED,
    /** 成交 */
    WON,
    /** 联系了但没成 */
    LOST,
    /** 误报，不是潜在客户（用于改进关键词和提示词） */
    IRRELEVANT
}
