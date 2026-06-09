package com.moli.langgraph.graph.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moli.langgraph.ai.service.MarketReportService;
import com.moli.langgraph.client.MarketReportApiClient;
import com.moli.langgraph.graph.NodeDetail;
import com.moli.langgraph.graph.graph.MarketReportGraphV3;
import com.moli.langgraph.graph.nodes.market.report.SummaryMergeNodeV3;
import com.moli.langgraph.graph.state.MarketReportStateV3;
import com.moli.langgraph.model.MarketReportReq;
import com.moli.langgraph.util.JsonUtil;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.streaming.StreamingOutput;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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
public class MarketReportAppV3 {

    private final MarketReportApiClient marketReportApiClient;
    private final MarketReportService marketReportService;
    private final StreamingChatModel streamingChatModel;
    private final ObjectMapper objectMapper = new ObjectMapper();


    public Flux<ServerSentEvent<String>> report(MarketReportReq marketReportReq) {
        MarketReportGraphV3 graph = new MarketReportGraphV3(marketReportApiClient, marketReportService);

        return Flux.<ServerSentEvent<String>>create(sink -> {
                    try {
                        // ===== 第一步：通过图执行预处理 + 路由决策，逐节点发送进度事件 =====
                        CompiledGraph<MarketReportStateV3> compiledGraph = graph.buildGraph();
                        Map<String, Object> initState = graph.buildInitState(marketReportReq);

                        MarketReportStateV3 state = runWithNodeEvents(compiledGraph, initState, sink);

                        if (state == null || StringUtils.isBlank(state.reportItemSummary())) {
                            sink.next(ServerSentEvent.<String>builder().data("指定时间范围内未获取到可总结的简报原文").build());
                            sink.complete();
                            return;
                        }

                        // ===== 第二步：根据图内路由决策的出口，分发不同的流式操作 =====
                        AtomicBoolean cancelled = new AtomicBoolean(false);
                        sink.onCancel(() -> cancelled.set(true));
                        sink.onDispose(() -> cancelled.set(true));

                        String exitNode = state.exitNode();
                        log.info("图内路由出口: {}", exitNode);

                        // 发送流式生成节点事件
                        NodeDetail nodeDetail = new NodeDetail();
                        nodeDetail.setName("streaming");
                        nodeDetail.setMessage("数据生产中");
                        nodeDetail.setData("");
                        nodeDetail.setStatus("running");
                        nodeDetail.setCostTime("0秒");
                        String s = JsonUtil.obj2String(nodeDetail);
                        log.info("###{}",s);
                        sink.next(ServerSentEvent.<String>builder().data(s).build());

                        ChatRequest chatRequest = switch (exitNode) {
                            case MarketReportStateV3.EXIT_FULL_REPORT -> {
                                log.info("分发流式操作: 详细格式化合并报告");
                                yield SummaryMergeNodeV3.buildMergeRequest(state);
                            }
                            case MarketReportStateV3.EXIT_BRIEF_REPORT -> {
                                log.info("分发流式操作: 简要摘要");
                                yield SummaryMergeNodeV3.buildBriefRequest(state);
                            }
                            default -> {
                                sink.next(ServerSentEvent.<String>builder().data("未知的路由出口: " + exitNode).build());
                                sink.complete();
                                yield null;
                            }
                        };

                        if (chatRequest == null) return;

                        // ===== 第三步：统一执行流式输出（绕过 graph.stream() 缓存） =====
                        streamChatToSink(chatRequest, streamingChatModel, sink, cancelled);

                    } catch (GraphStateException e) {
                        log.error("构建图失败", e);
                        sink.error(e);
                    } catch (Exception e) {
                        log.error("执行预处理失败", e);
                        sink.error(e);
                    }
                }, FluxSink.OverflowStrategy.IGNORE).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 执行图预处理，逐节点发送 running/done 进度事件
     */
    private MarketReportStateV3 runWithNodeEvents(CompiledGraph<MarketReportStateV3> compiledGraph,
                                                  Map<String, Object> initState,
                                                  FluxSink<ServerSentEvent<String>> sink) throws Exception {
        MarketReportStateV3 finalState = null;
        long start = System.currentTimeMillis();
        long end;
        for (var output : compiledGraph.stream(initState)) {
            if (output instanceof NodeOutput<?> nodeOutput) {
                end = System.currentTimeMillis();
                finalState = (MarketReportStateV3) nodeOutput.state();
                String node = output.node();
                NodeDetail nodeDetail = new NodeDetail();
                nodeDetail.setName(MarketReportStateV3.NODE_LABELS.get(node));
                nodeDetail.setMessage("");
                nodeDetail.setData(JsonUtil.obj2String(finalState.data().get(MarketReportStateV3.NODE_DATA.get(node))));
                nodeDetail.setStatus("done");
                nodeDetail.setCostTime((end - start) / 1000d + "秒");
                start = end;
                String data = JsonUtil.obj2String(nodeDetail);
                ServerSentEvent<String> build = ServerSentEvent.builder(data).build();
                sink.next(build);
                log.info("###{}", data);
            }
            else if (output instanceof StreamingOutput<MarketReportStateV3> state) {
                log.info("###{}", state.chunk());
                ServerSentEvent<String> build = ServerSentEvent.builder(state.chunk()).build();
                sink.next(build);
            }
        }
        return finalState;
    }

    /**
     * 将事件序列化为 JSON 字符串
     */
    private String toJson(String type, Map<String, Object> fields) {
        try {
            Map<String, Object> event = new LinkedHashMap<>(fields);
            event.put("type", type);
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            log.warn("JSON序列化失败", e);
            return "{\"type\":\"" + type + "\"}";
        }
    }

    /**
     * 通用的流式输出方法：将 StreamingChatModel 的流式输出直接推送到 FluxSink
     * <p>
     * 绕过 graph.stream() 的内部缓存机制，实现真正的打字机效果。
     * 所有出口都可以复用这个方法。
     */
    private static void streamChatToSink(ChatRequest chatRequest,
                                         StreamingChatModel streamingChatModel,
                                         FluxSink<ServerSentEvent<String>> sink,
                                         AtomicBoolean cancelled) {
        ObjectMapper mapper = new ObjectMapper();
        streamingChatModel.chat(chatRequest, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String chunk) {
                if (!cancelled.get() && StringUtils.isNotEmpty(chunk)) {
                    ServerSentEvent.Builder<String> builder = ServerSentEvent.builder();
                    try {
                        String json = mapper.writeValueAsString(Map.of("type", "content", "data", chunk));
                        log.info("###{}",json);
                        sink.next(builder.data(json).build());
                    } catch (Exception e) {
                        // fallback: 手动转义特殊字符
                        String escaped = chunk.replace("\\", "\\\\")
                                .replace("\"", "\\\"")
                                .replace("\n", "\\n")
                                .replace("\r", "\\r")
                                .replace("\t", "\\t");
                        String s = "{\"type\":\"content\",\"data\":\"" + escaped + "\"}";
                        log.info("###{}",s , e);
                        sink.next(builder.data(s).build());
                    }
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                if (!cancelled.get()) {
                    try {
                        ServerSentEvent.Builder<String> builder = ServerSentEvent.builder();
                        sink.next(builder.data(mapper.writeValueAsString(Map.of("type", "node", "name", "streaming", "label", "生成报告", "status", "done"))).build());
                        sink.next(builder.data(mapper.writeValueAsString(Map.of("type", "done"))).build());
                    } catch (Exception ignored) {
                    }
                    sink.complete();
                }
            }

            @Override
            public void onError(Throwable error) {
                if (!cancelled.get()) {
                    log.error("流式输出异常", error);
                    sink.error(error);
                }
            }
        });
    }
}
