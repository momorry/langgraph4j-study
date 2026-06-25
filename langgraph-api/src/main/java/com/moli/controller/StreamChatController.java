package com.moli.controller;

import com.moli.common.model.StreamChatReq;
import com.moli.service.StreamChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@RestController
public class StreamChatController {


    private final StreamChatService streamChatService;

    @PostMapping(value = "/stream-chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@RequestBody StreamChatReq streamChatReq) {
        // 禁用缓冲（必要）
//        response.getHeaders().set("X-Accel-Buffering", "no");
//        response.getHeaders().set("Cache-Control", "no-cache");
        return streamChatService.streamChat3(streamChatReq);
    }


}
