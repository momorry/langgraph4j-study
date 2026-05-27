package com.moli.langgraph.controller;

import com.moli.langgraph.model.MarketReportReq;
import com.moli.langgraph.graph.workflow.MarketReportAppV1;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/market-report")
@RequiredArgsConstructor
public class MarketReportController {

    private final MarketReportAppV1 marketReportAppV1;

    @PostMapping(value = "/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generate(@RequestBody MarketReportReq request) {
        return marketReportAppV1.genMarketReport(request);
    }
}
