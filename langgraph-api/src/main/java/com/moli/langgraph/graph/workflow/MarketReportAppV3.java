package com.moli.langgraph.graph.workflow;

import com.moli.langgraph.ai.service.MarketReportService;
import com.moli.langgraph.client.MarketReportApiClient;
import com.moli.langgraph.graph.graph.MarketReportGraph;
import com.moli.langgraph.graph.nodes.market.report.SummaryMergeNodeV3;
import com.moli.langgraph.graph.state.MarketReportStateV3;
import com.moli.langgraph.model.MarketReportReq;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

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

    public Flux<String> report(MarketReportReq marketReportReq) {
        MarketReportGraph graph = new MarketReportGraph(marketReportApiClient, marketReportService);

        return Flux.<String>create(sink -> {
            try {
                // ===== 第一步：通过图执行预处理 + 路由决策 =====
                CompiledGraph<MarketReportStateV3> compiledGraph = graph.buildGraph();
                Map<String, Object> initState = graph.buildInitState(marketReportReq);
                MarketReportStateV3 state = graph.runPreprocessGraph(compiledGraph, initState);

                if (state == null || StringUtils.isBlank(state.reportItemSummary())) {
                    sink.next("指定时间范围内未获取到可总结的简报原文。");
                    sink.complete();
                    return;
                }

                // ===== 第二步：根据图内路由决策的出口，分发不同的流式操作 =====
                AtomicBoolean cancelled = new AtomicBoolean(false);
                sink.onCancel(() -> cancelled.set(true));
                sink.onDispose(() -> cancelled.set(true));

                String exitNode = state.exitNode();
                log.info("图内路由出口: {}", exitNode);

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
                        // 未知出口或无数据，直接完成
                        sink.next("未知的路由出口: " + exitNode);
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
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 通用的流式输出方法：将 StreamingChatModel 的流式输出直接推送到 FluxSink
     * <p>
     * 绕过 graph.stream() 的内部缓存机制，实现真正的打字机效果。
     * 所有出口都可以复用这个方法。
     */
    private static void streamChatToSink(ChatRequest chatRequest,
                                         StreamingChatModel streamingChatModel,
                                         FluxSink<String> sink,
                                         AtomicBoolean cancelled) {
        streamingChatModel.chat(chatRequest, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String chunk) {
                if (!cancelled.get() && StringUtils.isNotEmpty(chunk)) {
                    sink.next(chunk);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                if (!cancelled.get()) {
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
