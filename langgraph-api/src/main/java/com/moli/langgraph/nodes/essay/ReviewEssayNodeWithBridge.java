package com.moli.langgraph.nodes.essay;

import com.moli.langgraph.ai.handler.StreamingBridge;
import com.moli.langgraph.state.EssayReviewState;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 教师批改作文节点 - 使用 StreamingBridge 实现流式输出
 * 
 * 示例：展示如何在 LangGraph 节点中使用 StreamingBridge 进行流式输出
 * 
 * @author likethewind
 * @since 2026/5/22
 */
@Slf4j
@RequiredArgsConstructor
public class ReviewEssayNodeWithBridge implements NodeAction<EssayReviewState> {

    public static final String REVIEW_ESSAY = "review_essay";

    private static final String SYSTEM_PROMPT = """
            你是一位经验丰富、富有爱心的语文教师。请阅读学生提交的作文，并给出详细的评语。
            
            评语要求：
            1. 先总体评价（优点为主，鼓励学生）
            2. 从以下几个方面进行点评：
               - 主题立意：是否切题，思想深度
               - 结构布局：开头、中间、结尾是否合理
               - 语言表达：用词、句式、修辞手法
               - 内容充实：事例、论据是否恰当
            3. 指出不足之处，并给出具体的修改建议
            4. 给出等级评定（优秀/良好/中等/需改进）
            5. 最后用温暖鼓励的话语结尾
            
            语气要求：亲切、专业、建设性、鼓励学生
            """;

    private final StreamingChatModel streamingChatModel;

    @Override
    public Map<String, Object> apply(EssayReviewState state) {
        log.info("执行节点: 教师批改作文 - 学生: {}, 题目: {}", state.studentName(), state.essayTopic());
        
        if (state.essayContent().isEmpty()) {
            return Map.of(
                    EssayReviewState.TEACHER_COMMENT_KEY, "未收到学生提交的作文。",
                    EssayReviewState.ESSAY_GRADE_KEY, "未评分"
            );
        }

        log.info("开始流式输出教师评语（使用 StreamingBridge）...");
        
        // 构建批改请求
        ChatRequest chatRequest = buildReviewRequest(state);
        
        // 使用 StreamingBridge 桥接流式输出
        // 注意：在节点中使用时，需要将 Flux 转换为同步结果或存储到状态中
        Flux<String> streamingFlux = StreamingBridge.bridge(streamingChatModel, chatRequest);
        
        // 方案1：收集所有流式输出并返回（同步方式）
        String fullComment = streamingFlux.collectList()
                .map(list -> String.join("", list))
                .block(); // 阻塞等待流式输出完成
        
        log.info("教师评语生成完成，长度: {}", fullComment != null ? fullComment.length() : 0);
        
        // 方案2：将 Flux 存储到状态中，由外部消费（异步方式）
        // 这需要在 State 中添加一个 Flux<String> 字段
        // return Map.of("reviewFlux", streamingFlux);
        
        return Map.of(
                EssayReviewState.TEACHER_COMMENT_KEY, fullComment != null ? fullComment : "",
                EssayReviewState.ESSAY_GRADE_KEY, extractGrade(fullComment)
        );
    }

    /**
     * 构建批改请求
     */
    public static ChatRequest buildReviewRequest(EssayReviewState state) {
        String userPrompt = String.format("""
                学生姓名：%s
                作文题目：《%s》
                
                作文内容：
                %s
                
                请仔细阅读以上作文，并给出详细的评语和等级评定。
                """, state.studentName(), state.essayTopic(), state.essayContent());

        return ChatRequest.builder()
                .messages(
                        SystemMessage.from(SYSTEM_PROMPT),
                        UserMessage.from(userPrompt)
                )
                .build();
    }

    /**
     * 从评语中提取等级
     */
    private String extractGrade(String comment) {
        if (comment == null) return "未评分";
        if (comment.contains("优秀")) return "优秀";
        if (comment.contains("良好")) return "良好";
        if (comment.contains("中等")) return "中等";
        if (comment.contains("需改进")) return "需改进";
        return "已评分";
    }
}
