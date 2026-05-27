package com.moli.langgraph.graph.nodes.market.report;

import com.moli.langgraph.ai.service.MarketReportService;
import com.moli.langgraph.graph.state.MarketReportStateV3;
import com.moli.langgraph.model.McpThsReportDetailDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author likethewind
 * @since 2026/5/26 15:55
 *
 */
@Slf4j
@RequiredArgsConstructor
public class SummaryItemNodeV3 implements NodeAction<MarketReportStateV3> {

    private final MarketReportService marketReportService;

    @Override
    public Map<String, Object> apply(MarketReportStateV3 marketReportStateV3) throws Exception {
        List<McpThsReportDetailDto> detailDtos = marketReportStateV3.reportItemDetail();

        StringBuilder totalSummary = new StringBuilder();
        Map<String, List<McpThsReportDetailDto>> rptGrpMap = detailDtos.stream().collect(Collectors.groupingBy(McpThsReportDetailDto::getReportId));
        rptGrpMap.forEach((reportId, v) -> {
            String groupDetail = v.stream()
                    .map(McpThsReportDetailDto::getFormatReportContent)
                    .collect(Collectors.joining("\n"));
            String summary = marketReportService.summary(groupDetail);
            totalSummary.append(summary).append("\n");
        });
        return Map.of(MarketReportStateV3.REPORT_ITEM_SUMMARY, totalSummary);
    }
}
