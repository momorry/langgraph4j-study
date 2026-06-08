package com.moli.langgraph.graph.graph;

import com.moli.langgraph.ai.service.MarketReportService;
import com.moli.langgraph.client.MarketReportApiClient;
import com.moli.langgraph.graph.nodes.market.report.QueryReportsDetailNodeV3;
import com.moli.langgraph.graph.nodes.market.report.SummaryItemNodeV3;
import com.moli.langgraph.graph.nodes.market.report.SummaryMergeNodeV2;
import com.moli.langgraph.graph.nodes.market.report.SummaryMergeNodeV3;
import com.moli.langgraph.graph.state.MarketReportStateV3;
import com.moli.langgraph.model.MarketReportReq;
import com.moli.langgraph.util.JsonUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.streaming.StreamingOutput;
import reactor.core.publisher.FluxSink;

import java.util.HashMap;
import java.util.Map;

import static org.bsc.langgraph4j.GraphDefinition.END;
import static org.bsc.langgraph4j.GraphDefinition.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 *
 * @author likethewind
 * @since 2026/5/26 17:13
 *
 */
@Data
@Slf4j
@RequiredArgsConstructor
public class MarketReportGraphV2 {

    private final MarketReportApiClient marketReportApiClient;
    private final MarketReportService marketReportService;

    /**
     * 执行图的前置节点（不包含流式输出节点）
     */
    public void runPreprocessGraph(CompiledGraph<MarketReportStateV3> graph,
                                   Map<String, Object> initState, FluxSink<String> sink) {
        for (var output : graph.stream(initState)) {
            if (output instanceof NodeOutput<?> nodeOutput) {
                if (output instanceof StreamingOutput<MarketReportStateV3> streaming) {
                    String chunk = streaming.chunk();
                    log.info("stream chunk:{}", chunk);
                    sink.next(chunk);
                } else {
//                    sink.next(JsonUtil.obj2String(output.state().data()));
                }
            }
        }
        sink.complete();
    }

    /**
     * 构建工作流图
     */
    public CompiledGraph<MarketReportStateV3> buildGraph() throws GraphStateException {
        return new StateGraph<>(MarketReportStateV3.SCHEMA, MarketReportStateV3::new)
                // 添加节点
                .addNode("query_reports", node_async(new QueryReportsDetailNodeV3(marketReportApiClient)))
                .addNode("summary_item", node_async(new SummaryItemNodeV3(marketReportService)))
                .addNode("summary_merge", node_async(new SummaryMergeNodeV2(marketReportService)))

                // 添加边
                .addEdge(START, "query_reports")
                .addEdge("query_reports", "summary_item")
                .addEdge("summary_item", "summary_merge")
                .addEdge("summary_merge", END)
                // 编译图
                .compile();
    }

    /**
     * 构建初始状态
     */
    public Map<String, Object> buildInitState(MarketReportReq reportReq) {
        Map<String, Object> state = new HashMap<>();
        state.put(MarketReportStateV3.START_DATE, reportReq.getStartDate());
        state.put(MarketReportStateV3.END_DATE, reportReq.getEndDate());
        state.put(MarketReportStateV3.STOCK_CODES, String.join("\n", reportReq.getStockCodes()));
        return state;
    }

}
