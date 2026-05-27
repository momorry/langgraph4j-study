package com.moli.langgraph.graph.workflow;

import com.moli.langgraph.ai.handler.StreamingBridge;
import com.moli.langgraph.graph.nodes.essay.AssignEssayNode;
import com.moli.langgraph.graph.nodes.essay.ReviewEssayNodeWithBridge;
import com.moli.langgraph.graph.nodes.essay.SubmitEssayNode;
import com.moli.langgraph.graph.state.EssayReviewState;
import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.StateGraph;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

import static org.bsc.langgraph4j.GraphDefinition.END;
import static org.bsc.langgraph4j.GraphDefinition.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 教师作文批改工作流应用
 * 
 * 工作流：
 * 1. 布置作文作业
 * 2. 学生提交作文
 * 3. 教师批改作文（流式输出评语）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EssayReviewApp {

    private final StreamingChatModel streamingChatModel;

    /**
     * 执行作文批改工作流，返回真正的流式输出
     * 
     * 关键：先执行图的前置节点，然后使用 StreamingBridge 桥接流式输出
     */
    public Flux<String> reviewEssay(String studentName, String essayTopic) {
        try {
            CompiledGraph<EssayReviewState> graph = buildGraph();
            Map<String, Object> initState = buildInitState(studentName, essayTopic);

            // 第一步：执行图的前置节点（布置作业、提交作文）
            EssayReviewState state = runPreprocessGraph(graph, initState);
            
            if (state == null || state.essayContent().isEmpty()) {
                return Flux.just("未收到学生提交的作文。");
            }

            // 第二步：使用 StreamingBridge 桥接流式输出
            log.info("LLM 开始流式输出评语，学生: {}", state.studentName());
            var chatRequest = ReviewEssayNodeWithBridge.buildReviewRequest(state);
            
            return StreamingBridge.bridge(streamingChatModel, chatRequest);
            
        } catch (GraphStateException e) {
            log.error("构建工作流图失败", e);
            return Flux.error(e);
        } catch (Exception e) {
            log.error("执行作文批改工作流失败", e);
            return Flux.error(e);
        }
    }

    /**
     * 执行图的前置节点（不包含流式输出节点）
     */
    private EssayReviewState runPreprocessGraph(CompiledGraph<EssayReviewState> graph, Map<String, Object> initState) throws Exception {
        EssayReviewState latestState = null;
        for (var output : graph.stream(initState)) {
            if (output instanceof NodeOutput<?> nodeOutput) {
                latestState = (EssayReviewState) nodeOutput.state();
//                log.info("执行节点: {}", nodeOutput.node());
            }
        }
        return latestState;
    }

    /**
     * 构建工作流图
     */
    private CompiledGraph<EssayReviewState> buildGraph() throws GraphStateException {
        return new StateGraph<>(EssayReviewState.SCHEMA, EssayReviewState::new)
                // 添加节点
                .addNode("assign_essay", node_async(new AssignEssayNode()))
                .addNode("submit_essay", node_async(new SubmitEssayNode()))
                .addNode("review_essay", node_async(new ReviewEssayNodeWithBridge(streamingChatModel)))
                
                // 添加边
                .addEdge(START, "assign_essay")
                .addEdge("assign_essay", "submit_essay")
                .addEdge("submit_essay", "review_essay")
                .addEdge("review_essay", END)
                
                // 编译图
                .compile();
    }

    /**
     * 构建初始状态
     */
    private Map<String, Object> buildInitState(String studentName, String essayTopic) {
        Map<String, Object> state = new HashMap<>();
        state.put(EssayReviewState.STUDENT_NAME_KEY, studentName);
        state.put(EssayReviewState.ESSAY_TOPIC_KEY, essayTopic);
        return state;
    }
}
