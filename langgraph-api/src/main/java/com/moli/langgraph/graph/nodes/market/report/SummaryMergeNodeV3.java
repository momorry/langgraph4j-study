package com.moli.langgraph.graph.nodes.market.report;

import com.moli.langgraph.graph.state.MarketReportStateV3;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;

/**
 * 合并总结节点 V3 - 工具类
 * <p>
 * 提供多个出口的 ChatRequest 构建方法，供图外流式分发使用：
 * <ul>
 *   <li>{@link #buildMergeRequest(MarketReportStateV3)} — 详细格式化合并报告（出口 full_report）</li>
 *   <li>{@link #buildBriefRequest(MarketReportStateV3)} — 简要一段话摘要（出口 brief_report）</li>
 * </ul>
 * 扩展新出口时，只需在此类添加对应的 buildXxxRequest 方法即可。
 *
 * @author likethewind
 * @since 2026/5/26 16:26
 */
public final class SummaryMergeNodeV3 {

    private static final String SYSTEM_PROMPT = """
            ## 技能：按照要求的格式整理输出内容

            - 需要列出所有给定的原文数据，不能遗漏。
            - 不存在公告的公司不需要列出。
            - 相同公司的数据进行合并，特别强调：合并后注意溯源编号的准确性，保证编号对应的公司名称与标记处的是一致的，并且可能需要你重新索引编号。
            - **严禁出现"张冠李戴"的情况**：在生成每一段内容时，必须再次核对角标编号是否属于当前这家公司。例如，不要将属于"中再资环"的角标 `[2]` 错误地标注在"天奇股份"的内容上。 - 如果某条信息涉及多个来源，请合并角标（如 `[1][3]`）。
            - 末尾列出引用的公告标题，用于溯源，必须携带有序编号，如1，2，3......
            - 提取的内容后附上公告标题编号：确保使用数字"<span class='reference-marker'>编号</span>"标记引用，如"示例句子<span class='reference-marker'>1</span>"。每句话至少引用一个搜索结果。若需引用多个结果，请分别标记，如"示例句子<span class='reference-marker'>1</span><span class='reference-marker'>2</span>"
            - 严格按照"格式A"输出内容，无需额外的阐述（注："示例A"仅作格式参考，禁止直接使用示例内容填充回答。）


            ## 格式A

            <b>【智慧数助手 · 市场简报】</b>
            🌟公司公告
            【股票名称1】股票名称1的公告内容总结<span class='reference-marker'>1</span>。
            【股票名称2】股票名称2的公告内容总结<span class='reference-marker'>2</span>。

            公告列表：
            【重点】
            仅存在公告数据时，文件链接存在 并且 报告过期=否：
            <a href="文章链接" target="_blank" id="reportId">1、文章标题1</a>
            仅存在公告数据时，文章链接不存在时 并且 报告过期=否：
            <a href="None" target="_blank" id="reportId">2、文章标题2</a>
            仅存在公告数据时，文件链接存在 并且 报告过期=是：
            <a href="文章链接" target="_blank" id="reportId" class='disabled'>1、文章标题1</a>
            仅存在公告数据时，文章链接不存在时 并且 报告过期=是：
            <a href="None" target="_blank" id="reportId" class='disabled'>2、文章标题2</a>
            """;

    /**
     * 简要摘要的 System Prompt（数据稀少时使用）
     */
    private static final String BRIEF_SYSTEM_PROMPT = """
            你是证券市场研究助手。请用一段简洁的中文摘要概括以下市场信息。
            要求：
            - 不超过 200 字
            - 提及关键个股名称及核心事件
            - 客观简洁，不编造
            """;

    private SummaryMergeNodeV3() {}

    /**
     * 出口 full_report：构建详细格式化合并报告的 ChatRequest
     */
    public static ChatRequest buildMergeRequest(MarketReportStateV3 state) {
        String reportItemSummary = state.reportItemSummary();
        return ChatRequest.builder()
                .messages(
                        SystemMessage.from(SYSTEM_PROMPT),
                        UserMessage.from("以下待合并信息：\n" + reportItemSummary))
                .build();
    }

    /**
     * 出口 brief_report：构建简要摘要的 ChatRequest
     */
    public static ChatRequest buildBriefRequest(MarketReportStateV3 state) {
        String reportItemSummary = state.reportItemSummary();
        return ChatRequest.builder()
                .messages(
                        SystemMessage.from(BRIEF_SYSTEM_PROMPT),
                        UserMessage.from("以下是少量市场信息，请用一段话简要概括：\n" + reportItemSummary))
                .build();
    }
}
