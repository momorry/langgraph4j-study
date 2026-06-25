package com.moli.langchain.config;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
public class ToolConfig {

//    @Bean
    public ToolProvider toolProvider() {
        DefaultToolExecutor toolExecutor = DefaultToolExecutor.builder().object("").build();
        return (toolProviderRequest) -> {
            if (toolProviderRequest.userMessage().singleText().contains("天气")) {
                ToolSpecification toolSpecification = ToolSpecification.builder()
                        .name("getWeather")
                        .description("返回预订详情")
                        .parameters(JsonObjectSchema.builder()
                                .addStringProperty("bookingNumber")
                                .build())
                        .build();
                return ToolProviderResult.builder()
                        .add(toolSpecification, toolExecutor)
                        .build();
            } else {
                return null;
            }
        };
    }

}
