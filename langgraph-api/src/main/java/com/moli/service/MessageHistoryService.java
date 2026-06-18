package com.moli.service;

import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

public interface MessageHistoryService {

    List<ChatMessage> queryMessages(Long appId, Long chatId, Integer limit);

    boolean addMessage(Long appId, Long chatId, String messageType ,String message, String thinkMessage);


}
