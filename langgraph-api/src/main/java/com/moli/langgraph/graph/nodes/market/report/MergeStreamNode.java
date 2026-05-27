package com.moli.langgraph.graph.nodes.market.report;

import com.moli.langgraph.graph.LangGraphStreaming;
import com.moli.langgraph.graph.state.MarketReportState;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.bsc.langgraph4j.langchain4j.generators.StreamingChatGenerator;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class MergeStreamNode implements NodeAction<MarketReportState> {

    public static final String MERGE_REPORT = "merge_report";

    private static final String SYSTEM_PROMPT = """
            你是证券市场研究助手。用户会提供多篇已总结的市场简报片段。
            请将「个股名称相同」的内容合并为一条（名称一致即视为同一标的），输出最终市场简报。
            格式建议：
            ## 市场简报（起止日期见用户输入）
            ### {个股名称}（{代码}）
            - 合并后的核心观点
            - 风险提示
            要求：中文、简洁客观、不重复、不编造。
            """;

    private final StreamingChatModel streamingChatModel;

    public static ChatRequest buildMergeRequest(MarketReportState state) {
        String mergedInput = state.pieceSummaries().stream()
                .map(s -> "---\n" + s)
                .collect(Collectors.joining("\n"));

        String userPrompt = """
                时间范围: %s ~ %s
                关注个股代码: %s
                
                以下为各篇单条 LLM 总结，请合并相同个股后输出最终简报：
                
                %s
                """.formatted(
                state.value(MarketReportState.START_DATE_KEY).orElse(""),
                state.value(MarketReportState.END_DATE_KEY).orElse(""),
                String.join(",", state.stockCodes()),
                mergedInput);

        return ChatRequest.builder()
                .messages(SystemMessage.from(SYSTEM_PROMPT), UserMessage.from(userPrompt))
                .build();
    }

    public static String buildMerge(MarketReportState state) {
        String mergedInput = state.pieceSummaries().stream()
                .map(s -> "---\n" + s)
                .collect(Collectors.joining("\n"));

        String userPrompt = """
                时间范围: %s ~ %s
                关注个股代码: %s
                
                以下为各篇单条 LLM 总结，请合并相同个股后输出最终简报：
                
                %s
                """.formatted(
                state.value(MarketReportState.START_DATE_KEY).orElse(""),
                state.value(MarketReportState.END_DATE_KEY).orElse(""),
                String.join(",", state.stockCodes()),
                mergedInput);

        return userPrompt;
    }

    @Override
    public Map<String, Object> apply(MarketReportState state) {
        if (state.pieceSummaries().isEmpty()) {
            return Map.of(MarketReportState.ANSWER_KEY, "指定时间范围内未获取到可总结的简报原文。");
        }

        log.info("LLM 流式合并总结，输入片段数: {}", state.pieceSummaries().size());

        var generator = StreamingChatGenerator.<MarketReportState>builder()
                .mapResult(response -> Map.of(MarketReportState.ANSWER_KEY, response.aiMessage().text()))
                .startingNode(MERGE_REPORT)
                .startingState(state)
                .build();
        streamingChatModel.chat(buildMergeRequest(state), generator.handler());
        return LangGraphStreaming.streamingMessages(generator);

//        MergeReportService mergeReportService = AiServices.create(MergeReportService.class, streamingChatModel);
//        Flux<String> stringFlux = mergeReportService.mergeReport(buildMerge(state));
//        return Map.of("_streaming_messages", stringFlux);
    }
}
