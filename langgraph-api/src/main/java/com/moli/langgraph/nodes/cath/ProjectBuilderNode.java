package com.moli.langgraph.nodes.cath;

import com.moli.langgraph.context.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;
@Slf4j
public class ProjectBuilderNode {
    public static AsyncNodeAction create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 项目构建");
            // TODO: 实际执行项目构建逻辑
            // 简单的假数据
            String buildResultDir = "/tmp/build/fake-build";
            // 更新状态
            context.setCurrentStep("项目构建");
            context.setBuildResultDir(buildResultDir);
            log.info("项目构建完成，结果目录: {}", buildResultDir);
            return WorkflowContext.saveContext(context);
        });
    }
}