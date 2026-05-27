package com.moli.langgraph.graph.nodes.market.report;

import com.moli.langgraph.ai.service.MarketReportService;
import com.moli.langgraph.graph.state.MarketReportStateV3;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 *
 * @author likethewind
 * @since 2026/5/26 16:26
 *
 */
@Slf4j
@RequiredArgsConstructor
public class SummaryMergeNodeV3 implements NodeAction<MarketReportStateV3> {

    private final MarketReportService marketReportService;

    @Override
    public Map<String, Object> apply(MarketReportStateV3 marketReportStateV3) throws Exception {
        String reportItemSummary = marketReportStateV3.reportItemSummary();
        Flux<String> flux = marketReportService.summaryMerge(reportItemSummary);
        StringBuilder mergeSummary = new StringBuilder();
        flux.doOnNext(chunk -> mergeSummary.append(chunk));

        log.info("mergeSummary: {}", mergeSummary);
        //这里如何收集flux的所有内容，存储到上下文，但是同时又实时的流式输出到外部
        return Map.of(MarketReportStateV3.REPORT_SUMMARY_MERGE, mergeSummary);
    }
}
