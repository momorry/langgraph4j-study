package com.moli.langchain.config;

import com.moli.langchain.memory.MysqlChatMemoryStore;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatMemoryConfig {

    @Bean
    public ChatMemoryProvider chatMemoryProvider(MysqlChatMemoryStore mysqlChatMemoryStore) {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .chatMemoryStore(mysqlChatMemoryStore)
                .maxMessages(20)
                .build();
    }
}
