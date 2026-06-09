package com.moli.langgraph.graph.graph;

import com.moli.langgraph.ai.service.MarketReportService;
import com.moli.langgraph.client.MarketReportApiClient;
import com.moli.langgraph.graph.nodes.market.report.QueryReportsDetailNodeV3;
import com.moli.langgraph.graph.nodes.market.report.SummaryItemNode;
import com.moli.langgraph.graph.nodes.market.report.SummaryMergeNodeV2;
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
public class MarketReportGraphV2 {

    private final MarketReportApiClient marketReportApiClient;
    private final MarketReportService marketReportService;


    /**
     * 构建工作流图
     */
    public CompiledGraph<MarketReportStateV3> buildGraph() throws GraphStateException {
        return new StateGraph<>(MarketReportStateV3.SCHEMA, MarketReportStateV3::new)
                // 添加节点
                .addNode("query_reports", node_async(new QueryReportsDetailNodeV3(marketReportApiClient)))
                .addNode("summary_item", node_async(new SummaryItemNode(marketReportService)))
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
