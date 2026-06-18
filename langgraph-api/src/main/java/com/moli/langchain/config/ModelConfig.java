package com.moli.langchain.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ModelConfig {

    @Value("${langchain4j.open-ai.streaming-chat-model.base-url}")
    private String baseUrl;

    @Value("${langchain4j.open-ai.streaming-chat-model.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.streaming-chat-model.model-name}")
    private String modelName;

    @Bean
    public OpenAiStreamingChatModel openAiStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                // 可在此添加更多自定义配置，例如：
                // .customHeaders(Map.of("X-Custom-Header", "value"))
                // .temperature(0.7)
                // .maxTokens(4096)
                .build();
    }



}
