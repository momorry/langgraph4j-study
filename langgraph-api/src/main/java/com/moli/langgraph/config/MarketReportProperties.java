package com.moli.langgraph.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "market-report")
public class MarketReportProperties {

    private String reportsUrl = "http://smartdata-uat.gf.com.cn/kmpreport/api/v1/public-reports";
    private String reportDetailsUrl = "http://smartdata-uat.gf.com.cn/kmpreport/api/v1/public-reports-detail";
    private String reportDetailUrl = "http://smartdata-uat.gf.com.cn/kmpreport/api/v1/public-report-detail";
    private String token = "";
}
