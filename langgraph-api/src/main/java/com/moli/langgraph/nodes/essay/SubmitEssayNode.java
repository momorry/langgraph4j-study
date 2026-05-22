package com.moli.langgraph.nodes.essay;

import com.moli.langgraph.state.EssayReviewState;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.Map;

/**
 * 学生提交作文节点
 */
@Slf4j
public class SubmitEssayNode implements NodeAction<EssayReviewState> {

    @Override
    public Map<String, Object> apply(EssayReviewState state) {
        log.info("执行节点: 学生提交作文 - 学生: {}", state.studentName());
        
        // 这里模拟学生提交的作文内容
        // 实际应用中，作文内容应该从前端或数据库获取
        String sampleEssay = String.format("""
                《%s》
                
                作者：%s
                
                在这个快节奏的时代，我们常常忽略了生活中那些美好的细节。
                清晨的第一缕阳光，傍晚的最后一抹晚霞，都是大自然赐予我们的礼物。
                
                记得有一次，我独自走在乡间的小路上，微风拂过脸颊，带来了泥土的芬芳。
                路边的野花竞相开放，蝴蝶在花丛中翩翩起舞。
                那一刻，我感受到了前所未有的宁静与美好。
                
                生活中并不缺少美，缺少的是发现美的眼睛。
                当我们放慢脚步，用心去感受，就会发现世界原来是如此美好。
                
                让我们学会在忙碌中寻找宁静，在平凡中发现美好，
                这样的人生才会更加充实和有意义。
                """, state.essayTopic(), state.studentName());

        log.info("学生作文提交完成，字数: {}", sampleEssay.length());
        
        return Map.of(
                EssayReviewState.ESSAY_CONTENT_KEY, sampleEssay
        );
    }
}
