package com.moli.langgraph.graph.workflow;

import com.moli.langgraph.client.MarketReportApiClient;
import com.moli.langgraph.model.MarketReportReq;
import com.moli.langgraph.graph.nodes.market.report.MergeStreamNode;
import com.moli.langgraph.graph.nodes.market.report.QueryReportDetailNode;
import com.moli.langgraph.graph.nodes.market.report.QueryReportsNode;
import com.moli.langgraph.graph.nodes.market.report.SummarizePiecesNode;
import com.moli.langgraph.graph.state.MarketReportState;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bsc.async.AsyncGenerator;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.streaming.StreamingOutput;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;


/**
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketReportAppV1 {

    private final MarketReportApiClient apiClient;
    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;

    /**
     * 根据请求参数构建并执行 LangGraph 工作流，最终节点流式输出合并后的市场简报。
     */
    public Flux<String> genMarketReport(MarketReportReq marketReportReq) {
        try {
            //创建编译后的图
            CompiledGraph<MarketReportState> graph = buildGraph(marketReportReq);
            //创建检查点
            Map<String, Object> initState = buildInitState(marketReportReq);

            return Flux.<String>create(sink -> {
                        try {
                            //不知道为什么哪里有缓存，导致消息是一次性输出的 todo
                            boolean streamed = false;
                            AsyncGenerator<NodeOutput<MarketReportState>> stream = graph.stream(initState);
                            for (NodeOutput<MarketReportState> output : stream) {
                                if (output instanceof StreamingOutput<?> streaming
                                        && MergeStreamNode.MERGE_REPORT.equals(streaming.node())) {
                                    streamed = true;
                                    String chunk = streaming.chunk();
                                    log.info("stream chunk:{}", chunk);
                                    if (StringUtils.isNotEmpty(chunk)) {
                                        sink.next(chunk);
                                    }
                                } else if (output instanceof NodeOutput<?> nodeOutput
                                        && MergeStreamNode.MERGE_REPORT.equals(nodeOutput.node())) {
                                    MarketReportState finalState = (MarketReportState) nodeOutput.state();
                                    if (!streamed && StringUtils.isNotBlank(finalState.answer())) {
                                        log.info("block answer:{}", finalState.answer());
                                        sink.next(finalState.answer());
                                    }
                                }
                            }
                            sink.complete();
                        } catch (Exception e) {
                            sink.error(e);
                        }
                    });
        } catch (GraphStateException e) {
            return Flux.error(e);
        }
    }

    private CompiledGraph<MarketReportState> buildGraph(MarketReportReq req) throws GraphStateException {
        //创建状态图
        return new StateGraph<>(MarketReportState.SCHEMA, MarketReportState::new)
                .addNode("query_reports", node_async(new QueryReportsNode(apiClient, req)))
                .addNode("query_details", node_async(new QueryReportDetailNode(apiClient)))
                .addNode("summarize_pieces", node_async(new SummarizePiecesNode(streamingChatModel)))
                .addNode(MergeStreamNode.MERGE_REPORT, node_async(new MergeStreamNode(streamingChatModel)))
                .addEdge(START, "query_reports")
                .addEdge("query_reports", "query_details")
                .addEdge("query_details", "summarize_pieces")
                .addEdge("summarize_pieces", MergeStreamNode.MERGE_REPORT)
                .addEdge(MergeStreamNode.MERGE_REPORT, END)
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
