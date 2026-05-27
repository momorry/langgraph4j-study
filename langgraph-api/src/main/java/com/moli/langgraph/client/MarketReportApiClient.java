package com.moli.langgraph.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Maps;
import com.moli.langgraph.config.MarketReportProperties;
import com.moli.langgraph.model.McpThsReportDetailDto;
import com.moli.langgraph.model.PublicReportDetail;
import com.moli.langgraph.model.PublicReportItem;
import com.moli.langgraph.model.ResponseResult;
import com.moli.langgraph.util.HttpClientUtils;
import com.moli.langgraph.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketReportApiClient {

    private final MarketReportProperties properties;

    public List<PublicReportItem> queryReports(List<String> stockCodes, String startDate, String endDate) {
        Map<String, String> headers = Maps.newHashMap();
        headers.put("Authorization", properties.getToken());
        Map<String, Object> params = new HashMap<>();
        params.put("stockCodes", String.join(",", stockCodes));
        params.put("startDate", startDate);
        params.put("endDate", endDate);
        String body = HttpClientUtils.get(properties.getReportsUrl(), params, headers);
        return Objects.requireNonNull(JsonUtil.snakeCaseString2Obj(body, new TypeReference<ResponseResult<List<PublicReportItem>>>() {
        })).getData();
    }

    public PublicReportDetail queryDetail(PublicReportItem item) {
        String body = null;
        try {
            Map<String, String> headers = Maps.newHashMap();
            headers.put("Authorization", properties.getToken());
            body = HttpClientUtils.post(properties.getReportDetailUrl(), JsonUtil.obj2SnakeCaseString(item), headers);
            ResponseResult<PublicReportDetail> res = JsonUtil.snakeCaseString2Obj(body, new TypeReference<ResponseResult<PublicReportDetail>>() {
            });
            if (res != null && res.isSuccess()) {
                return res.getData();
            }
        } catch (Exception e) {
            log.error("{}", body, e);
        }
        return null;
    }

    public List<McpThsReportDetailDto> queryPublicReportsDetail(String startDate, String endDate, String stockCodes) {
        Map<String, String> headers = Maps.newHashMap();
        headers.put("Authorization", properties.getToken());
        Map<String, Object> params = new HashMap<>();
        params.put("stockCodes", String.join(",", stockCodes));
        params.put("startDate", startDate);
        params.put("endDate", endDate);
        String body = HttpClientUtils.get(properties.getReportDetailsUrl(), params, headers);
        ResponseResult<List<McpThsReportDetailDto>> res = JsonUtil.snakeCaseString2Obj(body, new TypeReference<ResponseResult<List<McpThsReportDetailDto>>>() {
        });
        if (res != null && res.isSuccess()) {
            return res.getData();
        }
        return null;
    }

}
