package com.moli.langgraph.graph.state;

import com.moli.langgraph.model.McpThsReportDetailDto;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;
import reactor.core.publisher.Flux;

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

    public MarketReportStateV3(Map<String, Object> initData) {
        super(initData);
    }

    public static Map<String, Channel<?>> SCHEMA = Map.of(
            START_DATE, Channels.base(() -> ""),
            END_DATE, Channels.base(() -> ""),
            STOCK_CODES, Channels.base(() -> ""),
            REPORT_ITEM_DETAIL, Channels.base(java.util.ArrayList::new),
            REPORT_ITEM_SUMMARY, Channels.base(() -> ""),
            REPORT_SUMMARY_MERGE, Channels.base(() -> "")
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

    @SuppressWarnings("unchecked")
    public List<McpThsReportDetailDto> reportItemDetail() {
        return value(REPORT_ITEM_DETAIL).map(v -> (List<McpThsReportDetailDto>) v).orElse(List.of());
    }

}
