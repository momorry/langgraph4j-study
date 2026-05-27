package com.moli.langgraph.graph.nodes.market.report;

import com.moli.langgraph.client.MarketReportApiClient;
import com.moli.langgraph.model.MarketReportReq;
import com.moli.langgraph.model.PublicReportItem;
import com.moli.langgraph.graph.state.MarketReportState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class QueryReportsNode implements NodeAction<MarketReportState> {

    private final MarketReportApiClient apiClient;
    private final MarketReportReq request;

    @Override
    public Map<String, Object> apply(MarketReportState state) {
        log.info("查询市场简报列表: codes={}, {} ~ {}", request.getStockCodes(), request.getStartDate(), request.getEndDate());
        List<PublicReportItem> items = apiClient.queryReports(
                request.getStockCodes(), request.getStartDate(), request.getEndDate());
        log.info("简报列表数量: {}", items.size());
        return Map.of(MarketReportState.REPORT_ITEMS_KEY, items);
    }
}
