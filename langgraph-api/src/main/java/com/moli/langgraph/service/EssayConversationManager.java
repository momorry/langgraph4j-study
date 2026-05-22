package com.moli.langgraph.service;

import com.moli.langgraph.context.EssaySession;
import com.moli.langgraph.workflow.EssayReviewApp;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 作文批改对话管理器
 * 
 * 职责：
 * 1. 管理会话状态和记忆
 * 2. 判断是首次请求还是追问
 * 3. 协调工作流执行和对话继续
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EssayConversationManager {
    
    private final EssayReviewApp essayReviewApp;
    private final StreamingChatModel streamingChatModel;
    
    /**
     * 会话存储（生产环境应该使用 Redis）
     * key: sessionId, value: EssaySession
     */
    private final Map<String, EssaySession> sessionStore = new ConcurrentHashMap<>();
    
    /**
     * 处理用户消息（支持多轮对话）
     * 
     * @param sessionId 会话 ID
     * @param studentName 学生姓名（首次请求时提供）
     * @param essayTopic 作文题目（首次请求时提供）
     * @param userMessage 用户消息
     * @return 流式响应
     */
    public Flux<String> handleMessage(
            String sessionId,
            String studentName,
            String essayTopic,
            String userMessage) {
        
        // 1. 获取或创建会话
        EssaySession session = getOrCreateSession(sessionId, studentName, essayTopic);
        
        // 2. 添加用户消息到记忆
        session.addUserMessage(userMessage);
        
        log.info("处理消息 - 会话: {}, 状态: {}, 消息: {}", 
                sessionId, session.getState(), userMessage);
        
        // 3. 根据会话状态决定处理方式
        if (session.isFirstRequest()) {
            // 首次请求：执行完整工作流
            return handleInitialRequest(session, userMessage);
        } else if (session.isFollowUp()) {
            // 追问：使用记忆中的上下文继续对话
            return handleFollowUp(session, userMessage);
        } else {
            // 其他状态：执行工作流
            return handleInitialRequest(session, userMessage);
        }
    }
    
    /**
     * 处理首次请求（执行完整工作流）
     */
    private Flux<String> handleInitialRequest(EssaySession session, String userMessage) {
        log.info("首次请求，执行作文批改工作流 - 学生: {}, 题目: {}", 
                session.getStudentName(), session.getEssayTopic());
        
        // 更新状态
        session.setState(EssaySession.SessionState.SUBMITTED);
        
        // 使用 StringBuilder 收集完整评语
        final StringBuilder fullComment = new StringBuilder();
        
        return essayReviewApp.reviewEssay(session.getStudentName(), session.getEssayTopic())
            .doOnNext(token -> {
                // 收集完整评语
                fullComment.append(token);
            })
            .doOnComplete(() -> {
                // 工作流完成，保存结果到会话
                session.setTeacherComment(fullComment.toString());
                session.setEssayGrade(extractGrade(fullComment.toString()));
                session.setState(EssaySession.SessionState.REVIEWED);
                
                // 将完整评语添加到记忆
                session.addAiMessage(fullComment.toString());
                
                log.info("作文批改完成 - 会话: {}, 等级: {}", 
                        session.getSessionId(), session.getEssayGrade());
            })
            .doOnError(error -> {
                log.error("作文批改失败 - 会话: {}", session.getSessionId(), error);
                session.setState(EssaySession.SessionState.INITIAL);
            });
    }
    
    /**
     * 处理追问（使用记忆中的上下文对话）
     */
    private Flux<String> handleFollowUp(EssaySession session, String userMessage) {
        log.info("追问，使用对话历史 - 会话: {}", session.getSessionId());
        
        // 更新状态
        session.setState(EssaySession.SessionState.FOLLOWING_UP);
        
        final StringBuilder fullResponse = new StringBuilder();
        
        return Flux.<String>create(sink -> {
            try {
                // 构建包含对话历史的请求
                ChatRequest chatRequest = ChatRequest.builder()
                    .messages(session.getConversationHistory())
                    .build();
                
                AtomicBoolean cancelled = new AtomicBoolean(false);
                AtomicBoolean streamed = new AtomicBoolean(false);
                sink.onCancel(() -> cancelled.set(true));
                sink.onDispose(() -> cancelled.set(true));
                
                // 直接调用流式聊天模型
                streamingChatModel.chat(chatRequest, new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        if (cancelled.get() || StringUtils.isEmpty(partialResponse)) {
                            return;
                        }
                        streamed.set(true);
                        fullResponse.append(partialResponse);
                        log.debug("emit chunk at {}, length: {}", 
                                LocalDateTime.now(), partialResponse.length());
                        sink.next(partialResponse);
                    }
                    
                    @Override
                    public void onCompleteResponse(ChatResponse completeResponse) {
                        if (!cancelled.get() && !streamed.get()) {
                            String text = completeResponse.aiMessage().text();
                            if (StringUtils.isNotBlank(text)) {
                                fullResponse.append(text);
                                sink.next(text);
                            }
                        }
                        
                        if (!cancelled.get()) {
                            // 将 AI 回复添加到记忆
                            session.addAiMessage(fullResponse.toString());
                            session.setState(EssaySession.SessionState.REVIEWED);
                            
                            log.info("追问回答完成 - 会话: {}", session.getSessionId());
                            sink.complete();
                        }
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        if (!cancelled.get()) {
                            log.error("追问回答失败 - 会话: {}", session.getSessionId(), error);
                            sink.error(error);
                        }
                    }
                });
            } catch (Exception e) {
                log.error("处理追问失败 - 会话: {}", session.getSessionId(), e);
                sink.error(e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
    
    /**
     * 获取或创建会话
     */
    private EssaySession getOrCreateSession(
            String sessionId, 
            String studentName, 
            String essayTopic) {
        
        return sessionStore.computeIfAbsent(sessionId, id -> {
            log.info("创建新会话 - ID: {}, 学生: {}, 题目: {}", 
                    id, studentName, essayTopic);
            
            return EssaySession.builder()
                .sessionId(id)
                .studentName(studentName)
                .essayTopic(essayTopic)
                .state(EssaySession.SessionState.INITIAL)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        });
    }
    
    /**
     * 获取会话信息
     */
    public EssaySession getSession(String sessionId) {
        return sessionStore.get(sessionId);
    }
    
    /**
     * 清除会话
     */
    public void clearSession(String sessionId) {
        sessionStore.remove(sessionId);
        log.info("清除会话 - ID: {}", sessionId);
    }
    
    /**
     * 从评语中提取等级（简单实现）
     */
    private String extractGrade(String comment) {
        if (comment.contains("优秀")) {
            return "优秀";
        } else if (comment.contains("良好")) {
            return "良好";
        } else if (comment.contains("中等")) {
            return "中等";
        } else if (comment.contains("需改进")) {
            return "需改进";
        }
        return "已评分";
    }
}
