package com.moli.langgraph.controller;

import com.moli.langgraph.graph.workflow.MarketReportAppV3;
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
public class MarketReportV3Controller {

    private final MarketReportAppV3 marketReportAppV3;

    @PostMapping(value = "/report-v3", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> report(@RequestBody MarketReportReq marketReportReq) {
        return marketReportAppV3.report(marketReportReq);
    }

}
