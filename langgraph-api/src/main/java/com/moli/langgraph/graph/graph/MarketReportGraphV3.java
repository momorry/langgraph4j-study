package com.moli.langgraph.graph.graph;

import com.moli.langgraph.ai.service.MarketReportService;
import com.moli.langgraph.client.MarketReportApiClient;
import com.moli.langgraph.graph.nodes.market.report.QueryReportsDetailNodeV3;
import com.moli.langgraph.graph.nodes.market.report.RouteMergeNode;
import com.moli.langgraph.graph.nodes.market.report.SummaryItemNode;
import com.moli.langgraph.graph.state.MarketReportStateV3;
import com.moli.langgraph.model.MarketReportReq;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;

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
public class MarketReportGraphV3 {

    private final MarketReportApiClient marketReportApiClient;
    private final MarketReportService marketReportService;

    /**
     * 执行图的预处理节点（query_reports + summary_item），返回预处理后的状态。
     * 合并总结的流式输出由调用方在图外部直接使用 StreamingChatModel 完成。
     */
    public MarketReportStateV3 runPreprocessGraph(CompiledGraph<MarketReportStateV3> graph,
                                                  Map<String, Object> initState) throws Exception {
        MarketReportStateV3 finalState = null;
        for (var output : graph.stream(initState)) {
            if (output.state() instanceof MarketReportStateV3 s) {
                finalState = s;
            }
        }
        return finalState;
    }

    /**
     * 构建工作流图
     * <p>
     * 图结构：START → query_reports → summary_item → route_merge → END
     * <p>
     * route_merge 节点根据数据量决策出口（写入 state.EXIT_NODE），
     * 图外的调用方据此分发不同的流式操作。
     */
    public CompiledGraph<MarketReportStateV3> buildGraph() throws GraphStateException {
        return new StateGraph<>(MarketReportStateV3.SCHEMA, MarketReportStateV3::new)
                // 添加节点：预处理 + 路由决策
                .addNode("query_reports", node_async(new QueryReportsDetailNodeV3(marketReportApiClient)))
                .addNode("summary_item", node_async(new SummaryItemNode(marketReportService)))
                .addNode("route_merge", node_async(new RouteMergeNode()))

                // 添加边
                .addEdge(START, "query_reports")
                .addEdge("query_reports", "summary_item")
                .addEdge("summary_item", "route_merge")
                .addEdge("route_merge", END)
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
