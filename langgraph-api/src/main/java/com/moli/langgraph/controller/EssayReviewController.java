package com.moli.langgraph.controller;

import com.moli.langgraph.context.EssaySession;
import com.moli.langgraph.service.EssayConversationManager;
import com.moli.langgraph.graph.workflow.EssayReviewApp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * 作文批改控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/essay")
@RequiredArgsConstructor
public class EssayReviewController {

    private final EssayConversationManager conversationManager;
    private final EssayReviewApp essayReviewApp;

    /**
     * 提交作文并获取流式评语（支持多轮对话）
     * WebFlux 下 Flux<String> 可真正逐帧推送，无缓冲问题。
     *
     * @param request 请求参数（包含 sessionId、学生姓名、作文题目、用户消息）
     * @return 流式评语
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody ChatRequest request) {
        // 如果没有 sessionId，自动生成一个
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
            log.info("生成新会话 ID: {}", sessionId);
        }
        
        log.info("收到作文批改请求 - 会话: {}, 学生: {}, 题目: {}, 消息: {}", 
                sessionId, request.getStudentName(), request.getEssayTopic(), request.getMessage());
        
        // 第一个事件返回 sessionId，让前端保存
        return Flux.just("data: {\"sessionId\":\"" + sessionId + "\"}\n\n")
            .concatWith(conversationManager.handleMessage(
                    sessionId,
                    request.getStudentName(),
                    request.getEssayTopic(),
                    request.getMessage()
            ));
    }

    /**
     * 获取会话信息
     */
    @GetMapping("/session/{sessionId}")
    public EssaySession getSession(@PathVariable String sessionId) {
        return conversationManager.getSession(sessionId);
    }

    /**
     * 清除会话
     */
    @DeleteMapping("/session/{sessionId}")
    public void clearSession(@PathVariable String sessionId) {
        conversationManager.clearSession(sessionId);
    }

    /**
     * 旧接口：提交作文并获取流式评语（不支持多轮对话，保持向后兼容）
     *
     * @param studentName 学生姓名
     * @param essayTopic  作文题目
     * @return 流式评语
     */
    @GetMapping(value = "/review", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> reviewEssay(
            @RequestParam String studentName,
            @RequestParam String essayTopic) {
        
        log.info("收到作文批改请求(旧接口) - 学生: {}, 题目: {}", studentName, essayTopic);
        
        return essayReviewApp.reviewEssay(studentName, essayTopic);
    }

    /**
     * 旧接口：POST 方式提交作文并获取流式评语（不支持多轮对话，保持向后兼容）
     *
     * @param request 请求参数
     * @return 流式评语
     */
    @PostMapping(value = "/review", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> reviewEssayPost(@RequestBody EssayReviewRequest request) {
        
        log.info("收到作文批改请求(旧接口-POST) - 学生: {}, 题目: {}", 
                request.getStudentName(), request.getEssayTopic());
        
        return essayReviewApp.reviewEssay(request.getStudentName(), request.getEssayTopic());
    }

    /**
     * 多轮对话请求参数
     */
    @lombok.Data
    public static class ChatRequest {
        /** 会话 ID（可选，首次请求可为空） */
        private String sessionId;
        /** 学生姓名（首次请求时提供） */
        private String studentName;
        /** 作文题目（首次请求时提供） */
        private String essayTopic;
        /** 用户消息（首次请求时为作文内容，后续为追问） */
        private String message;
    }

    /**
     * 旧接口：作文批改请求参数（保持向后兼容）
     */
    @lombok.Data
    public static class EssayReviewRequest {
        private String studentName;
        private String essayTopic;
    }
}
