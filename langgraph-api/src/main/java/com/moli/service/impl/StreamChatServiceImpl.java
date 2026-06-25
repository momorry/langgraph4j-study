package com.moli.service.impl;

import com.moli.common.model.StreamChatReq;
import com.moli.langchain.agent.GeneralAgent;
import com.moli.langchain.agent.NormalAgent;
import com.moli.langchain.tool.WeatherTools;
import com.moli.service.StreamChatService;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
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

    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;

    @Override
    public Flux<ServerSentEvent<String>> streamChat3(StreamChatReq streamChatReq) {
//        List<ToolSpecification> toolSpecifications = ToolSpecifications.toolSpecificationsFrom(WeatherTools.class);
//        NormalAgent normalAgent = AiServices.create(NormalAgent.class, streamingChatModel);
        NormalAgent normalAgent = AiServices.builder(NormalAgent.class).tools(new WeatherTools()).streamingChatModel(streamingChatModel)
                .build();
        TokenStream tokenStream = normalAgent.execute(streamChatReq.getQuestion());
        Flux<ServerSentEvent<String>> flux = Flux.<ServerSentEvent<String>>create(sink -> {
            tokenStream.onPartialThinking(partialThinking -> {
                        ServerSentEvent<String> event = ServerSentEvent.<String>builder()
                                .data(partialThinking.text()).build();
                        log.info("partialThinking:{}", partialThinking.text());
                        sink.next(event);
                    }).onPartialResponse(partialResponse -> {
                        log.info("partialResponse:{}", partialResponse);
                        ServerSentEvent<String> event = ServerSentEvent.<String>builder()
                                .data(partialResponse).build();
                        sink.next(event);

                    })
                    .onCompleteResponse(completeResponse -> {
                        log.info("completeResponse:{}", completeResponse);
                        ServerSentEvent<String> event = ServerSentEvent.<String>builder()
                                .event("done")
                                .data("")
                                .build();
                        sink.next(event);
                    })
                    .onToolExecuted(toolExecution -> {
                        log.info("toolExecution: {}", toolExecution);
                    }).onError(throwable -> {
                        log.info("", throwable);
                    }).start();

        }).subscribeOn(Schedulers.boundedElastic());

        return flux;
    }

}
