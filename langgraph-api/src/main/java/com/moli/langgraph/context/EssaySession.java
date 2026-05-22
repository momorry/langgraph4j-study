package com.moli.langgraph.context;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 作文批改会话上下文
 */
@Data
@Builder
public class EssaySession {
    
    /**
     * 会话 ID
     */
    private String sessionId;
    
    /**
     * 学生姓名
     */
    private String studentName;
    
    /**
     * 作文题目
     */
    private String essayTopic;
    
    /**
     * 作文内容
     */
    private String essayContent;
    
    /**
     * 教师评语（完整版）
     */
    private String teacherComment;
    
    /**
     * 作文等级
     */
    private String essayGrade;
    
    /**
     * 对话记忆（支持多轮对话）
     */
    @Builder.Default
    private MessageWindowChatMemory memory = MessageWindowChatMemory.withMaxMessages(20);
    
    /**
     * 会话状态
     */
    @Builder.Default
    private SessionState state = SessionState.INITIAL;
    
    /**
     * 创建时间
     */
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    /**
     * 更新时间
     */
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    /**
     * 会话状态枚举
     */
    public enum SessionState {
        /** 初始状态，等待用户输入 */
        INITIAL,
        /** 作文已提交，等待批改 */
        SUBMITTED,
        /** 批改完成 */
        REVIEWED,
        /** 追问中 */
        FOLLOWING_UP
    }
    
    /**
     * 添加用户消息到记忆
     */
    public void addUserMessage(String message) {
        memory.add(UserMessage.from(message));
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 添加 AI 消息到记忆
     */
    public void addAiMessage(String message) {
        memory.add(AiMessage.from(message));
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 获取对话历史
     */
    public List<ChatMessage> getConversationHistory() {
        return memory.messages();
    }
    
    /**
     * 是否是首次请求
     */
    public boolean isFirstRequest() {
        return state == SessionState.INITIAL;
    }
    
    /**
     * 是否是追问
     */
    public boolean isFollowUp() {
        return state == SessionState.REVIEWED || state == SessionState.FOLLOWING_UP;
    }
}
