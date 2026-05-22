package com.moli.langgraph.ai.handler;

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.FluxSink;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author likethewind
 * @since 2026/5/22 8:45
 *
 */
@Data
@Slf4j
public class NodeStreamingChatResponseHandler implements StreamingChatResponseHandler {

    private final FluxSink<String> sink;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicBoolean streamed = new AtomicBoolean(false);

    public NodeStreamingChatResponseHandler(FluxSink<String> sink) {
        this.sink = sink;
        sink.onCancel(() -> cancelled.set(true));
        sink.onDispose(() -> cancelled.set(true));
    }

    @Override
    public void onPartialResponse(String partialResponse) {
        if (cancelled.get() || StringUtils.isEmpty(partialResponse)) {
            return;
        }
        streamed.set(true);
        log.debug("emit chunk at {}, length: {}", LocalDateTime.now(), partialResponse.length());
        sink.next(partialResponse);
    }

    @Override
    public void onCompleteResponse(ChatResponse completeResponse) {
        if (!cancelled.get() && !streamed.get()) {
            String text = completeResponse.aiMessage().text();
            if (StringUtils.isNotBlank(text)) {
                sink.next(text);
            }
        }
        if (!cancelled.get()) {
            sink.complete();
        }
    }

    @Override
    public void onError(Throwable error) {
        if (!cancelled.get()) {
            sink.error(error);
        }
    }

}
