package com.moli.langgraph.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicReportItem  implements Serializable {

    @Serial
    private static final long serialVersionUID = 5805793032326707739L;
    private String stockCode;
    private String stockName;
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

    private String reportDate;
    /**
     * 报告过期：是 or 否
     */
    private String reportExpired;

    /**
     * 公告链接（同花顺的下载链接）
     */
    private String reportUrl;

    /**
     * 原始公告链接（交易所原文链接，请勿用此链接下载原文）
     */
    private String reportOriginalUrl;
}
