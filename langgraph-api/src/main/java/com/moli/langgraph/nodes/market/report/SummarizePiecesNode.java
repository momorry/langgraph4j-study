package com.moli.langgraph.nodes.market.report;

import com.moli.langgraph.model.PublicReportDetail;
import com.moli.langgraph.state.MarketReportState;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Slf4j
@RequiredArgsConstructor
public class SummarizePiecesNode implements NodeAction<MarketReportState> {

    private static final String SYSTEM_PROMPT = """
            你是证券市场研究助手。请根据用户提供的单篇市场简报原文，输出结构化摘要，包含：
            1. 个股名称与代码（若原文有）
            2. 核心观点（3-5 条要点）
            3. 风险提示（如有）
            要求简洁、客观，使用中文，不要编造原文没有的信息。
            """;

    private final StreamingChatModel streamingChatModel;

    @Override
    public Map<String, Object> apply(MarketReportState state) {
        List<String> summaries = new ArrayList<>();
        int index = 1;
        for (PublicReportDetail detail : state.reportDetails()) {
            if (StringUtils.isBlank(detail.getContent())) {
                log.warn("简报 {} 原文为空，跳过", detail.getId());
                continue;
            }
            String userPrompt = buildUserPrompt(detail, index++);
            log.info("LLM 单篇总结: id={}, stock={}", detail.getId(), detail.getStockName());
            String summary = summarize(detail, userPrompt);
            if (StringUtils.isNotBlank(summary)) {
                summaries.add(summary);
            }
        }
        log.info("单篇总结完成，共 {} 篇", summaries.size());
        return Map.of(MarketReportState.PIECE_SUMMARIES_KEY, summaries);
    }

    private String summarize(PublicReportDetail detail, String userPrompt) {
        ChatRequest request = ChatRequest.builder()
                .messages(SystemMessage.from(SYSTEM_PROMPT), UserMessage.from(userPrompt))
                .build();
        StringBuilder streamedText = new StringBuilder();
        CompletableFuture<String> future = new CompletableFuture<>();

        streamingChatModel.chat(request, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                if (StringUtils.isNotEmpty(partialResponse)) {
                    streamedText.append(partialResponse);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                String text = completeResponse.aiMessage().text();
                future.complete(StringUtils.defaultIfBlank(text, streamedText.toString()));
            }

            @Override
            public void onError(Throwable error) {
                future.completeExceptionally(error);
            }
        });

        try {
            return future.join();
        } catch (CompletionException e) {
            log.error("LLM 单篇总结失败: id={}, stock={}", detail.getId(), detail.getStockName(), e);
            throw e;
        }
    }

    private String buildUserPrompt(PublicReportDetail detail, int index) {
        return """
                【第 %d 篇】
                个股: %s (%s)
                标题: %s
                日期: %s

                原文:
                %s
                """.formatted(
                index,
                StringUtils.defaultString(detail.getStockName(), "未知"),
                StringUtils.defaultString(detail.getStockCode(), "-"),
                StringUtils.defaultString(detail.getReportTitle(), "-"),
                StringUtils.defaultString(detail.getReportDate(), "-"),
                detail.getContent());
    }
}
