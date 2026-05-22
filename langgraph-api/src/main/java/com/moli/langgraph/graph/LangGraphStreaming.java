package com.moli.langgraph.graph;

import java.util.Map;

public final class LangGraphStreaming {

    /**
     * 不是随意命名的。
     * "_streaming_messages" 是 LangGraph4j 内部约定的特殊 key。节点返回这个 key 时，框架会把 value 当作流式消息源处理，也就是你传进去的 StreamingChatGenerator。
     * 所以必须是：
     * return Map.of("_streaming_messages", generator);
     * 不能随便改成：
     * return Map.of("streamingMessages", generator);
     * return Map.of("messages", generator);
     * return Map.of("xxx", generator);
     * 否则 graph.stream(...) 大概率只会把它当普通字段处理，甚至因为 MarketReportState.SCHEMA 没有这个字段而无法正确产生 StreamingOutput。
     * 可以把它理解成框架保留字段：_streaming_messages 表示“这个节点有流式消息要输出”。
     */
    public static final String STREAMING_MESSAGES_KEY = "_streaming_messages";

    private LangGraphStreaming() {
    }

    public static Map<String, Object> streamingMessages(Object generator) {
        return Map.of(STREAMING_MESSAGES_KEY, generator);
    }
}