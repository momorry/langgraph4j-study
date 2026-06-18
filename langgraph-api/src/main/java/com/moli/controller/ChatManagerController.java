package com.moli.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moli.common.base.ResponseResult;
import com.moli.common.dao.entity.MoChat;
import com.moli.common.dao.entity.MoMessageHistory;
import com.moli.common.model.CreateChatReq;
import com.moli.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatManagerController {

    private final ChatService chatService;

    /**
     * 创建会话
     */
    @PostMapping("/create-chat")
    public ResponseResult<MoChat> createChat(@RequestBody CreateChatReq req) {
        MoChat chat = chatService.createChat(req);
        return ResponseResult.ok(chat);
    }

    /**
     * 分页查询会话列表
     */
    @GetMapping("/list")
    public ResponseResult<Page<MoChat>> listChats(
            @RequestParam Long appId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        Page<MoChat> page = chatService.listChats(appId, pageNum, pageSize);
        return ResponseResult.ok(page);
    }

    /**
     * 查询会话的消息历史
     */
    @GetMapping("/{chatId}/messages")
    public ResponseResult<List<MoMessageHistory>> listMessages(@PathVariable Long chatId) {
        List<MoMessageHistory> messages = chatService.listMessages(chatId);
        return ResponseResult.ok(messages);
    }
}
