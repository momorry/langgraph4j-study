package com.moli.langgraph.graph.nodes.market.report;

import com.moli.langgraph.graph.state.MarketReportStateV3;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.Map;

/**
 * 路由决策节点 —— 在图内部决定流式输出的出口
 * <p>
 * 根据单篇总结的数量/长度，决定使用哪种流式合并策略：
 * <ul>
 *   <li>{@code full_report} — 数据充足（≥2 篇或总长度 ≥500 字符），走详细格式化合并</li>
 *   <li>{@code brief_report} — 数据稀少，走简要一段话摘要</li>
 * </ul>
 * <p>
 * 路由结果写入 {@code state.EXIT_NODE}，图外的调用方据此分发不同的流式操作。
 * 这种模式可以扩展到任意多个出口（如增加 risk_alert、sector_overview 等）。
 *
 * @author likethewind
 * @since 2026/5/27
 */
@Slf4j
public class RouteMergeNode implements NodeAction<MarketReportStateV3> {

    /** 触发详细报告的最少总结篇数 */
    private static final int MIN_PIECES_FOR_FULL = 2;
    /** 触发详细报告的最少总结总字符数 */
    private static final int MIN_LENGTH_FOR_FULL = 500;

    @Override
    public Map<String, Object> apply(MarketReportStateV3 state) {
        String summary = state.reportItemSummary();
        if (summary == null || summary.isBlank()) {
            log.info("路由决策: 无数据");
            return Map.of();
        }

        // 简单按换行分段估算篇数（每篇总结以换行分隔）
        long pieceCount = summary.lines().filter(line -> !line.isBlank()).count();

        String exitNode;
        if (pieceCount >= MIN_PIECES_FOR_FULL || summary.length() >= MIN_LENGTH_FOR_FULL) {
            exitNode = MarketReportStateV3.EXIT_FULL_REPORT;
            log.info("路由决策: {} (篇数={}, 长度={})", exitNode, pieceCount, summary.length());
        } else {
            exitNode = MarketReportStateV3.EXIT_BRIEF_REPORT;
            log.info("路由决策: {} (篇数={}, 长度={})", exitNode, pieceCount, summary.length());
        }

        return Map.of(MarketReportStateV3.EXIT_NODE, exitNode);
    }
}
