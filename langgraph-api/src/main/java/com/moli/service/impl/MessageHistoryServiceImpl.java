package com.moli.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moli.common.dao.entity.MoMessageHistory;
import com.moli.common.dao.manager.MoMessageHistoryManager;
import com.moli.service.MessageHistoryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.utils.Lists;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class MessageHistoryServiceImpl implements MessageHistoryService {

    private final MoMessageHistoryManager moMessageHistoryManager;

    @Override
    public List<ChatMessage> queryMessages(Long appId, Long chatId, Integer limit) {
        QueryWrapper<MoMessageHistory> wrapper = new QueryWrapper<>();
        wrapper.eq("chat_id", chatId)
                .eq("app_id", appId)
                .orderBy(true, true, "sort");
        Page<MoMessageHistory> page = new Page<>();
        page.setSize(limit);
        page.setCurrent(1);
        Page<MoMessageHistory> moMessageHistoryPage = moMessageHistoryManager.page(page, wrapper);
        List<MoMessageHistory> messageHistories = moMessageHistoryPage.getRecords();

        List<ChatMessage> chatMessages = Lists.newArrayList();
        for (MoMessageHistory item : messageHistories) {
            String messageType = item.getMessageType();
            String message = item.getMessage();
            if ("ai".equalsIgnoreCase(messageType)) {
                chatMessages.add(AiMessage.from(message));
            } else if ("user".equalsIgnoreCase(messageType)) {
                chatMessages.add(UserMessage.from(message));
            }
        }
        return chatMessages;
    }

    @Override
    public boolean addMessage(Long appId, Long chatId, String messageType, String message, String thinkMessage) {
        MoMessageHistory msg = new MoMessageHistory();
        msg.setId(IdWorker.getId());
        msg.setAppId(appId);
        msg.setChatId(chatId);
        msg.setThinkMessage(thinkMessage);
        msg.setMessage(message);
        msg.setMessageType(messageType);
        msg.setSort(IdWorker.getId());
        return moMessageHistoryManager.save(msg);
    }
}
