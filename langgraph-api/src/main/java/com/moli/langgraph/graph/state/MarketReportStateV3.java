package com.moli.langgraph.graph.state;

import com.moli.langgraph.model.McpThsReportDetailDto;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.List;
import java.util.Map;

/**
 *
 * @author likethewind
 * @since 2026/5/26 15:35
 *
 */
public class MarketReportStateV3 extends AgentState {

    public static final String START_DATE = "startDate";
    public static final String END_DATE = "endDate";
    public static final String STOCK_CODES = "stockCodes";
    public static final String REPORT_ITEM_DETAIL = "reportItemDetail";
    public static final String REPORT_ITEM_SUMMARY = "reportItemSummary";
    public static final String REPORT_SUMMARY_MERGE = "reportSummaryMerge";
    public static final String _STREAMING_MESSAGES = "_streaming_messages";

    /**
     * 图的路由出口标识。由图内的路由节点（RouteMergeNode）决策后写入，
     * 图外的调用方（MarketReportAppV3）读取此字段来决定执行哪种流式操作。
     * <p>
     * 这种"图内决策 + 图外流式分发"的模式可以扩展到任意多个出口。
     */
    public static final String EXIT_NODE = "exitNode";

    /** 出口值：详细合并报告（数据充足时） */
    public static final String EXIT_FULL_REPORT = "full_report";
    /** 出口值：简要摘要（数据稀少时） */
    public static final String EXIT_BRIEF_REPORT = "brief_report";

    public MarketReportStateV3(Map<String, Object> initData) {
        super(initData);
    }

    public static Map<String, Channel<?>> SCHEMA = Map.of(
            START_DATE, Channels.base(() -> ""),
            END_DATE, Channels.base(() -> ""),
            STOCK_CODES, Channels.base(() -> ""),
            REPORT_ITEM_DETAIL, Channels.base(java.util.ArrayList::new),
            REPORT_ITEM_SUMMARY, Channels.base(() -> ""),
            REPORT_SUMMARY_MERGE, Channels.base(() -> ""),
            EXIT_NODE, Channels.base(() -> "")
    );


    public String startDate() {
        return value(START_DATE).map(Object::toString).orElse("");
    }

    public String endDate() {
        return value(END_DATE).map(Object::toString).orElse("");
    }

    public String stockCodes() {
        return value(STOCK_CODES).map(Object::toString).orElse("");
    }
    public String reportItemSummary() {
        return value(REPORT_ITEM_SUMMARY).map(Object::toString).orElse("");
    }

    public String reportSummaryMerge() {
        return value(REPORT_SUMMARY_MERGE).map(Object::toString).orElse("");
    }

    /**
     * 获取图的路由出口标识，用于图外分发流式操作
     */
    public String exitNode() {
        return value(EXIT_NODE).map(Object::toString).orElse("");
    }

    @SuppressWarnings("unchecked")
    public List<McpThsReportDetailDto> reportItemDetail() {
        return value(REPORT_ITEM_DETAIL).map(v -> (List<McpThsReportDetailDto>) v).orElse(List.of());
    }

}
