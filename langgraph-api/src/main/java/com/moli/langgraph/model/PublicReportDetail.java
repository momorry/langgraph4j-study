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
public class PublicReportDetail implements Serializable {

    @Serial
    private static final long serialVersionUID = 7473194378131355517L;
    private String id;
    private String stockCode;
    private String stockName;
    private String reportTitle;
    private String reportDate;
    /** 简报原文 */
    private String content;
}
