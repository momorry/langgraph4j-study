package com.moli.service;

import com.moli.common.model.StreamChatReq;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

public interface StreamChatService {

    Flux<ServerSentEvent<String>> streamChat2(StreamChatReq streamChatReq);

    Flux<ServerSentEvent<String>> streamChat3(StreamChatReq streamChatReq);
}
