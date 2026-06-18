package com.moli.service.impl;

import com.moli.common.model.StreamChatReq;
import com.moli.common.util.JsonUtil;
import com.moli.langchain.agent.GeneralAgent;
import com.moli.service.StreamChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

@RequiredArgsConstructor
@Service
public class StreamChatServiceImpl implements StreamChatService {

    private final GeneralAgent generalAgent;

    @Override
    public Flux<ServerSentEvent<String>> streamChat2(StreamChatReq streamChatReq) {

        String memoryId = streamChatReq.getAppId() + "_" + streamChatReq.getChatId();
        Flux<String> stringFlux = generalAgent.streamChat(memoryId, streamChatReq.getQuestion());

        // 将序列化和构建 SSE 的操作切换到 boundedElastic 线程池
        return stringFlux
//                .publishOn(Schedulers.boundedElastic())  // ⬅️ 关键：切换线程
                .map(chunk -> { // 同步 CPU 操作
                    return ServerSentEvent.<String>builder()
                            .data(chunk)
                            .build();
                })
                .concatWith(Mono.fromCallable(() ->
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("")
                                .build()
                )).subscribeOn(Schedulers.boundedElastic());
    }

}
