package com.moli.langgraph.ai.handler;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * 流式输出桥接器 - 可在任何节点中复用
 * 
 * 用途：将 StreamingChatModel 的流式输出桥接到 Flux，供节点使用
 * 
 * @author likethewind
 * @since 2026/5/22
 */
@Slf4j
public class StreamingBridge {

    /**
     * 桥接流式聊天模型到 Flux
     * 
     * @param streamingChatModel 流式聊天模型
     * @param chatRequest 聊天请求
     * @return 流式输出的 Flux
     */
    public static Flux<String> bridge(StreamingChatModel streamingChatModel, ChatRequest chatRequest) {
        return Flux.<String>create(sink -> {
            try {
                log.debug("开始流式桥接");
                
                NodeStreamingChatResponseHandler handler = new NodeStreamingChatResponseHandler(sink);
                
                streamingChatModel.chat(chatRequest, handler);
                
            } catch (Exception e) {
                log.error("流式桥接异常", e);
                sink.error(e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 桥接流式聊天模型到 Flux（带自定义错误处理）
     * 
     * @param streamingChatModel 流式聊天模型
     * @param chatRequest 聊天请求
     * @param errorHandler 自定义错误处理
     * @return 流式输出的 Flux
     */
    public static Flux<String> bridge(
            StreamingChatModel streamingChatModel, 
            ChatRequest chatRequest,
            java.util.function.Consumer<Throwable> errorHandler) {
        
        return Flux.<String>create(sink -> {
            try {
                log.debug("开始流式桥接（带错误处理）");
                
                NodeStreamingChatResponseHandler handler = new NodeStreamingChatResponseHandler(sink);
                
                streamingChatModel.chat(chatRequest, handler);
                
            } catch (Exception e) {
                log.error("流式桥接异常", e);
                if (errorHandler != null) {
                    errorHandler.accept(e);
                }
                sink.error(e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
