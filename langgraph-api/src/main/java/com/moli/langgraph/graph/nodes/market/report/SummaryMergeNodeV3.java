package com.moli.langgraph.graph.nodes.market.report;

import com.moli.langgraph.ai.service.MarketReportService;
import com.moli.langgraph.graph.LangGraphStreaming;
import com.moli.langgraph.graph.state.MarketReportStateV3;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.bsc.langgraph4j.langchain4j.generators.StreamingChatGenerator;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 合并总结节点 V3
 * <p>
 * 核心思路：将 LangChain4j AiService 返回的 Flux<String> 桥接到 LangGraph4j 的 StreamingChatGenerator，
 * 通过有界队列（LinkedBlockingDeque）实现背压和打字机效果：
 * <ul>
 *   <li>Flux 订阅在 boundedElastic 线程上运行，作为生产者，每收到一个 chunk 就通过 handler 推入有界队列</li>
 *   <li>LangGraph4j 的 graph.stream() 在框架线程上消费队列，逐个取出 chunk 产生 StreamingOutput</li>
 *   <li>队列满时生产者阻塞（背压），队列空时消费者等待，自然形成打字机节奏</li>
 * </ul>
 *
 * @author likethewind
 * @since 2026/5/26 16:26
 */
@Slf4j
@RequiredArgsConstructor
public class SummaryMergeNodeV3 implements NodeAction<MarketReportStateV3> {

    private final MarketReportService marketReportService;

    /**
     * 有界队列容量。适度大以避免生产者突发推入时溢出；
     * 配合 Flux 管道中的 delayElements 实现打字机效果。
     */
    private static final int QUEUE_CAPACITY = 10;

    /**
     * 每个 chunk 之间的最小间隔。用于控制打字机节奏，
     * 避免 LLM 突发输出导致队列堆积过快。可根据实际体验调优（20~50ms）。
     */
    private static final Duration CHUNK_DELAY = Duration.ofMillis(30);

    @Override
    public Map<String, Object> apply(MarketReportStateV3 state) throws Exception {
        String reportItemSummary = state.reportItemSummary();
        Flux<String> flux = marketReportService.summaryMerge(reportItemSummary);

        // 用于累积完整文本，供最终写入 state 使用
        StringBuilder accumulatedText = new StringBuilder();

        // 构建 StreamingChatGenerator，传入有界队列
        // mapResult: 当流结束时，将累积的完整文本写入 state 的 REPORT_SUMMARY_MERGE 字段
        var generator = StreamingChatGenerator.<MarketReportStateV3>builder()
                .mapResult(response -> Map.of(
                        MarketReportStateV3.REPORT_SUMMARY_MERGE, accumulatedText.toString()))
                .startingNode(MarketReportStateV3.REPORT_SUMMARY_MERGE)
                .startingState(state)
                .queue(new LinkedBlockingQueue<>(QUEUE_CAPACITY))
                .build();

        // 获取 generator 内部的 StreamingChatResponseHandler
        // 该 handler 的 onPartialResponse 会将 chunk 推入有界队列
        var handler = generator.handler();

        // 订阅 Flux，将每个 chunk 桥接到 handler（即推入有界队列）
        // delayElements: 在 Flux 管道中控制每个 chunk 的发射间隔，
        // 既防止有界队列被突发流量打满，又保持打字机节奏
        flux.subscribe(
                chunk -> {
                    // 生产者：每收到一个 chunk，累积并推入队列
                    accumulatedText.append(chunk);
                    handler.onPartialResponse(chunk);
                    log.info("##chunk:{}", chunk);
                },
                error -> {
                    // 异常处理：通知 handler 出错
                    log.error("summaryMerge Flux 异常", error);
                    handler.onError(error);
                },
                () -> {
                    // 流结束：构建一个包含完整文本的 ChatResponse 通知 handler 完成
                    // 这样 langgraph4j 框架知道流已结束，会触发 mapResult 写入 state
                    ChatResponse completeResponse = ChatResponse.builder()
                            .aiMessage(dev.langchain4j.data.message.AiMessage.from(accumulatedText.toString()))
                            .build();
                    handler.onCompleteResponse(completeResponse);
                    log.info("summaryMerge 流式输出完成，总长度: {}", accumulatedText.length());
                }
        );

        // 返回 generator 给 langgraph4j 框架
        // 框架会通过 graph.stream() 从有界队列中逐个消费 chunk，产生 StreamingOutput
        return LangGraphStreaming.streamingMessages(generator);
    }
}
