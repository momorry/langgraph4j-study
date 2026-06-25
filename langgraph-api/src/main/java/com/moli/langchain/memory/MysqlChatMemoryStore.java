package com.moli.langchain.memory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moli.common.dao.entity.MoMessageHistory;
import com.moli.common.dao.manager.MoMessageHistoryManager;
import dev.langchain4j.data.message.*;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MysqlChatMemoryStore implements ChatMemoryStore {

    private final MoMessageHistoryManager moMessageHistoryManager;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String[] ids = parseMemoryId(memoryId);
        Long appId = Long.parseLong(ids[0]);
        Long chatId = Long.parseLong(ids[1]);

        List<MoMessageHistory> records = moMessageHistoryManager.list(
                new LambdaQueryWrapper<MoMessageHistory>()
                        .eq(MoMessageHistory::getAppId, appId)
                        .eq(MoMessageHistory::getChatId, chatId)
                        .orderByAsc(MoMessageHistory::getSort)
        );

        List<ChatMessage> messages = new ArrayList<>();
        for (MoMessageHistory item : records) {
            if(StringUtils.isBlank(item.getMessage())) {
                continue;
            }
            switch (item.getMessageType().toLowerCase()) {
                case "user" -> messages.add(UserMessage.from(item.getMessage()));
                case "ai" -> messages.add(AiMessage.from(item.getMessage()));
//                case "system" -> messages.add(SystemMessage.from(item.getMessage()));
                default -> log.warn("Unknown messageType: {}", item.getMessageType());
            }
        }
        return messages;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String[] ids = parseMemoryId(memoryId);
        Long appId = Long.parseLong(ids[0]);
        Long chatId = Long.parseLong(ids[1]);

        // 先删除该会话的历史记录
        deleteMessages(memoryId);

        // 重新插入全部消息
        long sort = 0;
        for (ChatMessage msg : messages) {
            MoMessageHistory record = new MoMessageHistory();
            record.setAppId(appId);
            record.setChatId(chatId);
            record.setSort(++sort);
            boolean needSave = false;
            if (msg instanceof UserMessage userMessage) {
                record.setMessageType("user");
                record.setMessage(userMessage.singleText());
                needSave = true;
            } else if (msg instanceof AiMessage aiMessage) {
                record.setMessageType("ai");
                record.setMessage(aiMessage.text());
                needSave = true;
            } else if (msg instanceof SystemMessage systemMessage) {
                record.setMessageType("system");
                record.setMessage(systemMessage.text());
//                needSave = false;
            } else if(msg instanceof ToolExecutionResultMessage toolMsg) {
                log.info("工具调用");
            }
            log.info("###msg:{}", msg);
            log.info("###record:{}", record);
            if(needSave) {
                moMessageHistoryManager.save(record);
            }
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String[] ids = parseMemoryId(memoryId);
        Long appId = Long.parseLong(ids[0]);
        Long chatId = Long.parseLong(ids[1]);

        moMessageHistoryManager.remove(
                new LambdaQueryWrapper<MoMessageHistory>()
                        .eq(MoMessageHistory::getAppId, appId)
                        .eq(MoMessageHistory::getChatId, chatId)
        );
    }

    private String[] parseMemoryId(Object memoryId) {
        String id = String.valueOf(memoryId);
        String[] parts = id.split("_");
        if (parts.length != 2) {
            throw new IllegalArgumentException("memoryId 格式应为 'appId_chatId', 实际值: " + memoryId);
        }
        return parts;
    }
}
