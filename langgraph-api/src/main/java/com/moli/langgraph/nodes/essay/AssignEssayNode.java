package com.moli.langgraph.nodes.essay;

import com.moli.langgraph.state.EssayReviewState;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.Map;

/**
 * 教师布置作文作业节点
 */
@Slf4j
public class AssignEssayNode implements NodeAction<EssayReviewState> {

    private static final String SYSTEM_PROMPT = """
            你是一位经验丰富的语文教师。请根据给定的作文题目，为学生布置一篇作文作业。
            要求：
            1. 明确作文要求（字数、体裁等）
            2. 给出写作建议和注意事项
            3. 语言亲切鼓励
            """;

    @Override
    public Map<String, Object> apply(EssayReviewState state) {
        log.info("执行节点: 布置作文作业 - 题目: {}", state.essayTopic());
        
        // 这里模拟布置作业，实际可以调用 LLM 生成详细的作业要求
        String assignmentDetails = String.format("""
                作文题目：《%s》
                
                作业要求：
                1. 字数：800-1000字
                2. 体裁：记叙文或议论文
                3. 要求真情实感，条理清晰
                4. 注意开头引人入胜，结尾点题升华
                
                写作建议：
                - 可以先列提纲再写作
                - 注意使用恰当的修辞手法
                - 多用具体事例支撑观点
                
                请同学们认真写作，老师期待你们的佳作！
                """, state.essayTopic());

        log.info("作文作业布置完成");
        
        return Map.of(
                EssayReviewState.ESSAY_TOPIC_KEY, state.essayTopic(),
                "assignmentDetails", assignmentDetails
        );
    }
}
