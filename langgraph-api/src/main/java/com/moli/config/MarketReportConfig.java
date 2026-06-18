package com.moli.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MarketReportProperties.class)
public class MarketReportConfig {
}
