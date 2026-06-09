package com.moli.langgraph.controller;

import com.moli.langgraph.graph.workflow.MarketReportAppV2;
import com.moli.langgraph.model.MarketReportReq;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 *
 * @author likethewind
 * @since 2026/5/27 9:24
 *
 */
@RequiredArgsConstructor
@RestController
public class MarketReportV2Controller {

    private final MarketReportAppV2 marketReportAppV2;

    /**
     * 市场简报 V2 流式接口
     * WebFlux 下 Flux&lt;ServerSentEvent&gt; 可真正逐帧推送，无缓冲问题。
     *
     * @param marketReportReq
     * @return
     */
    @PostMapping(value = "/report-v2", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> report(@RequestBody MarketReportReq marketReportReq) {
        return marketReportAppV2.report(marketReportReq);
    }

}
