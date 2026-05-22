package com.moli.langgraph.nodes.market.report;

import com.moli.langgraph.client.MarketReportApiClient;
import com.moli.langgraph.model.PublicReportDetail;
import com.moli.langgraph.model.PublicReportItem;
import com.moli.langgraph.state.MarketReportState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class QueryReportDetailNode implements NodeAction<MarketReportState> {

    private final MarketReportApiClient apiClient;

    @Override
    public Map<String, Object> apply(MarketReportState state) {
        List<PublicReportDetail> details = new ArrayList<>();

        for (PublicReportItem item : state.reportItems()) {
            if (StringUtils.isBlank(item.getReportId())) {
                log.warn("跳过无 id 的简报项: {}", item);
                continue;
            }
            log.info("查询简报详情: id={}, stock={}", item.getReportId(), item.getStockName());
            PublicReportDetail detail = apiClient.queryDetail(item);
            if (Objects.isNull(detail)) {
                continue;
            }
            detail.setStockCode(item.getStockCode());
            detail.setStockName(item.getStockName());
            detail.setReportTitle(item.getReportTitle());
            detail.setReportDate(item.getReportDate());
            details.add(detail);
        }
        log.info("简报详情数量: {}", details.size());
        return Map.of(MarketReportState.REPORT_DETAILS_KEY, details);
    }
}
