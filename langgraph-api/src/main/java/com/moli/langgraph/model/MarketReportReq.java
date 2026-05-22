package com.moli.langgraph.model;

import lombok.Data;

import java.util.List;

/**
 *
 * @author likethewind
 * @since 2026/5/19 9:40
 *
 */
@Data
public class MarketReportReq {

    private String startDate;
    private String endDate;
    private List<String> stockCodes;
}
