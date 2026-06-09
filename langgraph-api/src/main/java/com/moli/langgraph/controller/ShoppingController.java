package com.moli.langgraph.controller;

import com.moli.langgraph.graph.workflow.ShoppingApp;
import com.moli.langgraph.model.ShoppingReq;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 购物流程控制器
 *
 * @author moli
 * @since 2026/6/8
 */
@RequiredArgsConstructor
@RestController
public class ShoppingController {

    private final ShoppingApp shoppingApp;

    /**
     * 购物流程流式接口
     * WebFlux 下 Flux&lt;ServerSentEvent&gt; 可真正逐帧推送，无缓冲问题。
     * @param shoppingReq
     * @return
     */
    @PostMapping(value = "/shopping", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> shopping(@RequestBody ShoppingReq shoppingReq) {
        return shoppingApp.shopping(shoppingReq);
    }
}
