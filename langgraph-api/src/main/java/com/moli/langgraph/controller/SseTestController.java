package com.moli.langgraph.controller;

import com.moli.langgraph.graph.NodeDetail;
import com.moli.langgraph.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * SSE 纯 For 循环测试控制器
 * <p>
 * 不依赖 langgraph4j，仅用纯 for 循环 + sleep 模拟节点执行。
 * <p>
 * WebFlux 下使用 Flux&lt;ServerSentEvent&lt;T&gt;&gt; 原生支持逐帧 flush，
 * ServerSentEventHttpMessageWriter 会对每个事件显式格式化并立即 flush，
 * 无需像 MVC 那样依赖 SseEmitter + 手动 flush()。
 *
 * @author moli
 * @since 2026/6/8
 */
@Slf4j
@RestController
public class SseTestController {

    private static final List<String> NODE_NAMES = List.of(
            "节点A-浏览", "节点B-加购", "节点C-支付", "节点D-发货", "节点E-收货"
    );

    @PostMapping(value = "/sse-test", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> sseTest() {
        log.info("SSE测试开始");

        // 每个节点依次执行：先发 running，等 2 秒，再发 done
        // 使用 concatMap 保证节点串行执行（flatMap 会并发）
        return Flux.fromIterable(NODE_NAMES)
                .concatMap(name -> {
                    // running 事件立即发送
                    ServerSentEvent<String> runningEvent = ServerSentEvent.<String>builder()
                            .data(JsonUtil.obj2String(buildDetail(name, "running", "处理中...", "", "")))
                            .build();

                    // done 事件延迟 2 秒后发送
                    ServerSentEvent<String> doneEvent = ServerSentEvent.<String>builder()
                            .data(JsonUtil.obj2String(buildDetail(name, "done", "", "{\"result\":\"success\"}", "2.00秒")))
                            .build();

                    // 先发 running，等 2 秒后发 done
                    return Flux.just(runningEvent)
                            .concatWith(Mono.just(doneEvent).delayElement(Duration.ofSeconds(2)));
                })
                // 所有节点完成后，发送 done 总事件
                .concatWith(Flux.just(
                        ServerSentEvent.<String>builder()
                                .data(JsonUtil.obj2String(Map.of("type", "done")))
                                .build()
                ))
                .doOnComplete(() -> log.info("SSE测试完成"));
    }

    private NodeDetail buildDetail(String name, String status, String message, String data, String costTime) {
        NodeDetail detail = new NodeDetail();
        detail.setName(name);
        detail.setStatus(status);
        detail.setMessage(message);
        detail.setData(data);
        detail.setCostTime(costTime);
        return detail;
    }
}
