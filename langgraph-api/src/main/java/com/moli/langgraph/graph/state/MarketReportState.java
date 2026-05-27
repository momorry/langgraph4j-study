package com.moli.langgraph.graph.state;

import com.moli.langgraph.model.PublicReportDetail;
import com.moli.langgraph.model.PublicReportItem;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 定义状态上下文，可在每个节点读取这些上下文数据，以及赋值
 */
public class MarketReportState extends AgentState {

    public static final String START_DATE_KEY = "startDate";
    public static final String END_DATE_KEY = "endDate";
    public static final String STOCK_CODES_KEY = "stockCodes";
    public static final String REPORT_ITEMS_KEY = "reportItems";
    public static final String REPORT_DETAILS_KEY = "reportDetails";
    public static final String PIECE_SUMMARIES_KEY = "pieceSummaries";
    public static final String ANSWER_KEY = "answer";

    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            START_DATE_KEY, Channels.base(() -> ""),
            END_DATE_KEY, Channels.base(() -> ""),
            STOCK_CODES_KEY, Channels.base(ArrayList::new),
            REPORT_ITEMS_KEY, Channels.base(ArrayList::new),
            REPORT_DETAILS_KEY, Channels.base(ArrayList::new),
            PIECE_SUMMARIES_KEY, Channels.base(ArrayList::new),
            ANSWER_KEY, Channels.base(() -> "")
    );

    public MarketReportState(Map<String, Object> initData) {
        super(initData);
    }

    @SuppressWarnings("unchecked")
    public List<String> stockCodes() {
        return value(STOCK_CODES_KEY).map(v -> (List<String>) v).orElse(List.of());
    }

    @SuppressWarnings("unchecked")
    public List<PublicReportItem> reportItems() {
        return value(REPORT_ITEMS_KEY).map(v -> (List<PublicReportItem>) v).orElse(List.of());
    }

    @SuppressWarnings("unchecked")
    public List<PublicReportDetail> reportDetails() {
        return value(REPORT_DETAILS_KEY).map(v -> (List<PublicReportDetail>) v).orElse(List.of());
    }

    @SuppressWarnings("unchecked")
    public List<String> pieceSummaries() {
        return value(PIECE_SUMMARIES_KEY).map(v -> (List<String>) v).orElse(List.of());
    }

    public String answer() {
        return value(ANSWER_KEY).map(Object::toString).orElse("");
    }
}
