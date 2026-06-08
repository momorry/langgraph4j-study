package com.moli.langgraph.graph.nodes.market.report;

import com.moli.langgraph.client.MarketReportApiClient;
import com.moli.langgraph.graph.state.MarketReportStateV3;
import com.moli.langgraph.model.McpThsReportDetailDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class QueryReportsDetailNodeV3 implements NodeAction<MarketReportStateV3> {

    private final MarketReportApiClient apiClient;

    @Override
    public Map<String, Object> apply(MarketReportStateV3 state) {
        String startDate = state.startDate();
        String endDate = state.endDate();
        String stockCodes = state.stockCodes();
        log.info("查询市场简报列表: stockCodes={}, {} ~ {}", stockCodes, startDate,endDate);
        List<McpThsReportDetailDto> detailDtos = apiClient.queryPublicReportsDetail( startDate, endDate, stockCodes);
        log.info("简报列表数量: {}", detailDtos.size());
        return Map.of(MarketReportStateV3.REPORT_ITEM_DETAIL, detailDtos);
    }
}
