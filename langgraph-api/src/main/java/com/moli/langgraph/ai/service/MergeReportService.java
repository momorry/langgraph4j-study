package com.moli.langgraph.ai.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

/**
 *
 * @author likethewind
 * @since 2026/5/21 9:46
 *
 */
public interface MergeReportService {

    @SystemMessage("""
            你是证券市场研究助手。用户会提供多篇已总结的市场简报片段。
            请将「个股名称相同」的内容合并为一条（名称一致即视为同一标的），输出最终市场简报。
            格式建议：
            ## 市场简报（起止日期见用户输入）
            ### {个股名称}（{代码}）
            - 合并后的核心观点
            - 风险提示
            要求：中文、简洁客观、不重复、不编造。
            """)
    @UserMessage("{{userMessage}}")
    Flux<String> mergeReport(String userMessage);
}
