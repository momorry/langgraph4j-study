package com.moli.langgraph.workflow;

import com.moli.langgraph.client.MarketReportApiClient;
import com.moli.langgraph.model.MarketReportReq;
import com.moli.langgraph.nodes.market.report.MergeStreamNode;
import com.moli.langgraph.nodes.market.report.QueryReportDetailNode;
import com.moli.langgraph.nodes.market.report.QueryReportsNode;
import com.moli.langgraph.nodes.market.report.SummarizePiecesNode;
import com.moli.langgraph.state.MarketReportState;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.StateGraph;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.bsc.langgraph4j.GraphDefinition.END;
import static org.bsc.langgraph4j.GraphDefinition.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;


@Slf4j
@Service
@RequiredArgsConstructor
public class MarketReportApp {

    private final MarketReportApiClient apiClient;
    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;

    /**
     * 根据请求参数构建并执行 LangGraph 工作流，最终合并阶段直接桥接 StreamingChatModel，避免 graph.stream 缓存后集中吐出。
     */
    public Flux<String> genMarketReport(MarketReportReq marketReportReq) {
        try {
            CompiledGraph<MarketReportState> graph = buildGraph(marketReportReq);
            Map<String, Object> initState = buildInitState(marketReportReq);

            return Flux.<String>create(sink -> {
                        try {
                            MarketReportState state = runPreprocessGraph(graph, initState);
                            if (state == null || state.pieceSummaries().isEmpty()) {
                                sink.next("指定时间范围内未获取到可总结的简报原文。");
                                sink.complete();
                                return;
                            }

                            AtomicBoolean cancelled = new AtomicBoolean(false);
                            AtomicBoolean streamed = new AtomicBoolean(false);
                            sink.onCancel(() -> cancelled.set(true));
                            sink.onDispose(() -> cancelled.set(true));

                            log.info("LLM 直接流式合并总结，输入片段数: {}", state.pieceSummaries().size());
                            streamingChatModel.chat(MergeStreamNode.buildMergeRequest(state), new StreamingChatResponseHandler() {
                                @Override
                                public void onPartialResponse(String partialResponse) {
                                    if (cancelled.get() || StringUtils.isEmpty(partialResponse)) {
                                        return;
                                    }
                                    streamed.set(true);
                                    log.debug("emit merge chunk at {}, length: {}", System.currentTimeMillis(), partialResponse.length());
                                    sink.next(partialResponse);
                                }

                                @Override
                                public void onCompleteResponse(ChatResponse completeResponse) {
                                    if (!cancelled.get() && !streamed.get()) {
                                        String text = completeResponse.aiMessage().text();
                                        if (StringUtils.isNotBlank(text)) {
                                            sink.next(text);
                                        }
                                    }
                                    if (!cancelled.get()) {
                                        sink.complete();
                                    }
                                }

                                @Override
                                public void onError(Throwable error) {
                                    if (!cancelled.get()) {
                                        sink.error(error);
                                    }
                                }
                            });
                        } catch (Exception e) {
                            sink.error(e);
                        }
                    }).subscribeOn(Schedulers.boundedElastic());
        } catch (GraphStateException e) {
            return Flux.error(e);
        }
    }

    private MarketReportState runPreprocessGraph(CompiledGraph<MarketReportState> graph, Map<String, Object> initState) throws Exception {
        MarketReportState latestState = null;
        for (var output : graph.stream(initState)) {
            if (output instanceof NodeOutput<?> nodeOutput) {
                latestState = (MarketReportState) nodeOutput.state();
            }
        }
        return latestState;
    }

    private CompiledGraph<MarketReportState> buildGraph(MarketReportReq req) throws GraphStateException {
        return new StateGraph<>(MarketReportState.SCHEMA, MarketReportState::new)
                .addNode("query_reports", node_async(new QueryReportsNode(apiClient, req)))
                .addNode("query_details", node_async(new QueryReportDetailNode(apiClient)))
                .addNode("summarize_pieces", node_async(new SummarizePiecesNode(streamingChatModel)))
                .addEdge(START, "query_reports")
                .addEdge("query_reports", "query_details")
                .addEdge("query_details", "summarize_pieces")
                .addEdge("summarize_pieces", END)
                .compile();
    }

    private Map<String, Object> buildInitState(MarketReportReq req) {
        Map<String, Object> state = new HashMap<>();
        state.put(MarketReportState.START_DATE_KEY, req.getStartDate());
        state.put(MarketReportState.END_DATE_KEY, req.getEndDate());
        state.put(MarketReportState.STOCK_CODES_KEY, req.getStockCodes());
        return state;
    }
}