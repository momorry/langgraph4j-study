package com.moli.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moli.common.dao.entity.MoChat;
import com.moli.common.dao.entity.MoMessageHistory;
import com.moli.common.model.CreateChatReq;

import java.util.List;

/**
 * 会话管理服务
 */
public interface ChatService {

    /**
     * 创建会话
     */
    MoChat createChat(CreateChatReq req);

    /**
     * 分页查询会话列表
     */
    Page<MoChat> listChats(Long appId, int pageNum, int pageSize);

    /**
     * 查询会话的消息历史
     */
    List<MoMessageHistory> listMessages(Long chatId);
}
