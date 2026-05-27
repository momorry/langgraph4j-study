package com.moli.langgraph.model;

import lombok.Data;

import java.io.Serializable;

/**
 * @author likethewind
 * @since 2025/12/4 13:13
 */
@Data
public class McpThsReportDetailDto implements Serializable {

    /**
     * 是否重点
     */
    private boolean core;
    /**
     * 公告ID
     */
    private String reportId;
    /**
     * 公告标题
     */
    private String reportTitle;
    /**
     * 公告日期
     */
    private String reportDate;
    /**
     * 报告过期：是 or 否
     */
    private String reportExpired;

    /**
     * 股票代码
     */
    private String stockCode;

    /**
     * 股票名称
     */
    private String stockName;

    /**
     * 公告链接（同花顺的下载链接）
     */
    private String reportUrl;

    /**
     * 原始公告链接（交易所原文链接，请勿用此链接下载原文）
     */
    private String reportOriginalUrl;
    /**
     * 原文内容：格式化后
     */
    private String formatReportContent;

    private boolean isImportant;
}
