package com.moli.langgraph.graph.nodes.market.report;

import com.moli.langgraph.ai.service.MarketReportService;
import com.moli.langgraph.graph.state.MarketReportStateV3;
import com.moli.langgraph.model.McpThsReportDetailDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 *
 * @author likethewind
 * @since 2026/5/26 15:55
 *
 */
@Slf4j
@RequiredArgsConstructor
public class SummaryItemNodeV2Async implements NodeAction<MarketReportStateV3> {

    private final MarketReportService marketReportService;

    @Override
    public Map<String, Object> apply(MarketReportStateV3 marketReportStateV3) throws Exception {
        List<McpThsReportDetailDto> detailDtos = marketReportStateV3.reportItemDetail();


        StringBuilder totalSummary = new StringBuilder();
        Map<String, List<McpThsReportDetailDto>> rptGrpMap = detailDtos.stream().collect(Collectors.groupingBy(McpThsReportDetailDto::getReportId));

        // 为每个分组创建异步任务，转为 CompletableFuture 以便并行执行
        List<CompletableFuture<String>> futures = rptGrpMap.values().stream()
                .map(v -> {
                    String groupDetail = v.stream()
                            .map(McpThsReportDetailDto::getReportText)
                            .collect(Collectors.joining("\n"));
                    return marketReportService.asyncSummary(groupDetail)
                            .collectList()
                            .map(list -> list.stream().collect(Collectors.joining()))
                            .toFuture();
                })
                .collect(Collectors.toList());

        // 等待所有并行任务完成（join 不受 Reactor NIO 线程限制）
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 汇总所有结果
        futures.stream()
                .map(CompletableFuture::join)
                .forEach(r -> totalSummary.append(r).append("\n"));
        log.info("totalSummary:{}", totalSummary);
        return Map.of(MarketReportStateV3.REPORT_ITEM_SUMMARY, totalSummary);
    }
}
