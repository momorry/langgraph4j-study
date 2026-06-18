package com.moli.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moli.common.dao.entity.MoChat;
import com.moli.common.dao.entity.MoMessageHistory;
import com.moli.common.dao.manager.MoChatManager;
import com.moli.common.dao.manager.MoMessageHistoryManager;
import com.moli.common.model.CreateChatReq;
import com.moli.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 会话管理服务实现
 */
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final MoChatManager moChatManager;
    private final MoMessageHistoryManager moMessageHistoryManager;

    @Override
    public MoChat createChat(CreateChatReq req) {
        MoChat chat = new MoChat();
        chat.setAppId(req.getAppId());
        chat.setChatName(req.getChatName());
        chat.setModelName(req.getModelName());
        moChatManager.save(chat);
        return chat;
    }

    @Override
    public Page<MoChat> listChats(Long appId, int pageNum, int pageSize) {
        Page<MoChat> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MoChat> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MoChat::getAppId, appId)
                .orderByDesc(MoChat::getCreateTime);
        return moChatManager.page(page, wrapper);
    }

    @Override
    public List<MoMessageHistory> listMessages(Long chatId) {
        LambdaQueryWrapper<MoMessageHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MoMessageHistory::getChatId, chatId)
                .orderByAsc(MoMessageHistory::getSort);
        return moMessageHistoryManager.list(wrapper);
    }
}
