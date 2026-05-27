package com.moli.langgraph.graph.workflow;

import com.moli.langgraph.ai.service.MarketReportService;
import com.moli.langgraph.client.MarketReportApiClient;
import com.moli.langgraph.graph.graph.MarketReportGraph;
import com.moli.langgraph.graph.state.MarketReportStateV3;
import com.moli.langgraph.model.MarketReportReq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 *
 * @author likethewind
 * @since 2026/5/26 11:19
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketReportAppV3 {

    private final MarketReportApiClient marketReportApiClient;
    private final MarketReportService marketReportService;

    public Flux<String> report(MarketReportReq marketReportReq) {
        MarketReportGraph graph = new MarketReportGraph(marketReportApiClient, marketReportService);
        Flux<String> flux = Flux.<String>create(sink -> {
            try {
                CompiledGraph<MarketReportStateV3> compiledGraph = graph.buildGraph();
                Map<String, Object> state = graph.buildInitState(marketReportReq);
                graph.runPreprocessGraph(compiledGraph, state, sink);
            } catch (GraphStateException e) {
                log.error("", e);
            }
        });
        return flux;
    }

}
