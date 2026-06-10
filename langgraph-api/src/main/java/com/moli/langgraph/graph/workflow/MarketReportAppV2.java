package com.moli.langgraph.graph.workflow;

import com.moli.langgraph.ai.service.MarketReportService;
import com.moli.langgraph.client.MarketReportApiClient;
import com.moli.langgraph.graph.NodeDetail;
import com.moli.langgraph.graph.graph.MarketReportGraphV2;
import com.moli.langgraph.graph.state.MarketReportStateV3;
import com.moli.langgraph.model.MarketReportReq;
import com.moli.langgraph.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.async.AsyncGenerator;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.streaming.StreamingOutput;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * V3 版本市场简报应用 —— 多出口流式分发示例
 * <p>
 * 架构模式：图内决策路由 → 图外按出口分发流式操作
 * <pre>
 * 图内: START → query_reports → summary_item → route_merge → END
 *                                              (决策出口写入 state.EXIT_NODE)
 *
 * 图外: switch (state.exitNode()) {
 *     case "full_report"  → 详细格式化合并（流式）
 *     case "brief_report" → 简要一段话摘要（流式）
 *     default             → 无数据提示
 * }
 * </pre>
 * 扩展新出口时：
 * 1. 在 State 中添加新的 EXIT_xxx 常量
 * 2. 在 RouteMergeNode 中添加路由条件
 * 3. 在 SummaryMergeNodeV3 中添加 buildXxxRequest 方法
 * 4. 在下方 switch 中添加新的 case 分支
 *
 * @author likethewind
 * @since 2026/5/26 11:19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketReportAppV2 {

    private final MarketReportApiClient marketReportApiClient;
    private final MarketReportService marketReportService;


    public Flux<ServerSentEvent<String>> report(MarketReportReq marketReportReq) {
        MarketReportGraphV2 graph = new MarketReportGraphV2(marketReportApiClient, marketReportService);

        Flux<ServerSentEvent<String>> serverSentEventFlux = Flux.<ServerSentEvent<String>>create(sink -> {
            try {
                // ===== 构建图并流式执行 =====
                // 图结构：START → query_reports → summary_item → summary_merge → END
                // summary_merge 节点内部使用 StreamingChatGenerator，
                // 框架会自动将其输出转为 StreamingOutput（打字机效果）
                CompiledGraph<MarketReportStateV3> compiledGraph = graph.buildGraph();
                AsyncGenerator<NodeOutput<MarketReportStateV3>> stream =
                        compiledGraph.stream(graph.buildInitState(marketReportReq));

                long start = System.currentTimeMillis();
                long end;

                for (NodeOutput<MarketReportStateV3> nodeOutput : stream) {
                    if (nodeOutput instanceof StreamingOutput<MarketReportStateV3> streamingOutput) {
                        // ===== 流式 chunk 输出（打字机效果） =====
                        // SummaryMergeNodeV2 通过 StreamingChatGenerator 产生的流式数据
                        String chunk = streamingOutput.chunk();
                        if (chunk != null && !chunk.isEmpty()) {
                            String json = JsonUtil.obj2String(Map.of("type", "content", "data", chunk));
                            log.info("###{}", json);
                            sink.next(ServerSentEvent.<String>builder().data(json).build());
                        }
                    } else {
                        // ===== 节点完成事件 =====
                        // 每个非流式节点（query_reports / summary_item / summary_merge）完成时触发
                        end = System.currentTimeMillis();
                        String node = nodeOutput.node();
                        MarketReportStateV3 state = nodeOutput.state();

                        NodeDetail nodeDetail = new NodeDetail();
                        nodeDetail.setName(MarketReportStateV3.NODE_LABELS.getOrDefault(node, node));
                        nodeDetail.setMessage("");
                        String dataKey = MarketReportStateV3.NODE_DATA.get(node);
                        nodeDetail.setData(dataKey != null
                                ? JsonUtil.obj2String(state.data().get(dataKey))
                                : "");
                        nodeDetail.setStatus("done");
                        nodeDetail.setCostTime((end - start) / 1000d + "秒");
                        start = end;

                        String data = JsonUtil.obj2String(nodeDetail);
                        log.info("###{}", data);
                        sink.next(ServerSentEvent.<String>builder().data(data).build());
                    }
                }

                // ===== 流式输出结束，发送 done 事件 =====
                sink.next(ServerSentEvent.<String>builder()
                        .data(JsonUtil.obj2String(Map.of("type", "done"))).build());
                sink.complete();

            } catch (GraphStateException e) {
                log.error("构建图失败", e);
                sink.error(e);
            } catch (Exception e) {
                log.error("执行图流式输出失败", e);
                sink.error(e);
            }
        });

        serverSentEventFlux.subscribe();
        return serverSentEventFlux;
    }

}
