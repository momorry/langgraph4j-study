package com.moli.langchain.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

/**
 *
 * @author likethewind
 * @since 2026/5/26 15:57
 *
 */
@AiService
public interface MarketReportAgent {

    @SystemMessage(fromResource = "prompt/summary-report.md")
    @UserMessage("以下是公告信息：{{reports}}")
    String summary(@V("reports") String reports);

    @SystemMessage(fromResource = "prompt/summary-report.md")
    @UserMessage("以下是公告信息：{{reports}}")
    Flux<String> asyncSummary(@V("reports") String reports);

    @SystemMessage(fromResource = "prompt/summary-merge.md")
    @UserMessage("以下待合并信息：{{itemSummary}}")
    Flux<String> summaryMerge(@V("itemSummary") String itemSummary);
}
