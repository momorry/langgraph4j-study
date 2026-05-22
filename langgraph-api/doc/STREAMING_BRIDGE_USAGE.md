# 流式输出桥接器使用指南

## 概述

`StreamingBridge` 是一个通用的流式输出桥接器，可以在任何 LangGraph 节点中复用，将 `StreamingChatModel` 的流式输出转换为 Reactor `Flux`。

## 核心组件

### 1. NodeStreamingChatResponseHandler
- 实现了 `StreamingChatResponseHandler` 接口
- 处理流式输出的各个阶段（partial response、complete、error）
- 支持取消和 disposed 状态管理

### 2. StreamingBridge
- 静态工具类，提供便捷的桥接方法
- 将 `StreamingChatModel` 的流式输出转换为 `Flux<String>`
- 支持自定义错误处理

## 使用方式

### 方式一：在 Workflow 层使用（推荐）

在 Workflow 层先执行前置节点，然后使用 `StreamingBridge` 桥接流式输出：

```java
@Service
public class EssayReviewApp {
    
    private final StreamingChatModel streamingChatModel;
    
    public Flux<String> reviewEssay(String studentName, String essayTopic) {
        // 1. 执行前置节点
        EssayReviewState state = runPreprocessGraph(graph, initState);
        
        // 2. 构建聊天请求
        ChatRequest chatRequest = ReviewEssayNode.buildReviewRequest(state);
        
        // 3. 使用 StreamingBridge 桥接流式输出
        return StreamingBridge.bridge(streamingChatModel, chatRequest);
    }
}
```

**优点**：
- 真正实现流式输出，每个 chunk 立即推送
- 节点保持简单，只负责业务逻辑
- Workflow 层统一处理流式输出

### 方式二：在 Node 层使用（同步收集）

在节点内部使用 `StreamingBridge`，收集完整的流式输出后返回：

```java
public class ReviewEssayNodeWithBridge implements NodeAction<EssayReviewState> {
    
    private final StreamingChatModel streamingChatModel;
    
    @Override
    public Map<String, Object> apply(EssayReviewState state) {
        ChatRequest chatRequest = buildReviewRequest(state);
        
        // 桥接流式输出
        Flux<String> streamingFlux = StreamingBridge.bridge(streamingChatModel, chatRequest);
        
        // 收集所有输出（阻塞等待）
        String fullComment = streamingFlux.collectList()
                .map(list -> String.join("", list))
                .block();
        
        return Map.of(
            EssayReviewState.TEACHER_COMMENT_KEY, fullComment,
            EssayReviewState.ESSAY_GRADE_KEY, extractGrade(fullComment)
        );
    }
}
```

**注意**：这种方式会阻塞节点执行，直到流式输出完成，失去了流式的优势。

### 方式三：在 Node 层使用（异步存储）

将 `Flux` 存储到状态中，由外部消费：

```java
// 1. 在 State 中添加 Flux 字段
public record EssayReviewState(
    // ... 其他字段
    Flux<String> reviewFlux
) {
    public static final String REVIEW_FLUX_KEY = "reviewFlux";
}

// 2. 在节点中存储 Flux
public Map<String, Object> apply(EssayReviewState state) {
    ChatRequest chatRequest = buildReviewRequest(state);
    Flux<String> streamingFlux = StreamingBridge.bridge(streamingChatModel, chatRequest);
    
    return Map.of(EssayReviewState.REVIEW_FLUX_KEY, streamingFlux);
}

// 3. 在外部消费 Flux
EssayReviewState state = runGraph(graph, initState);
state.reviewFlux().subscribe(
    chunk -> System.out.println(chunk),
    error -> System.err.println(error),
    () -> System.out.println("完成")
);
```

## API 参考

### StreamingBridge.bridge(model, request)

最简单的桥接方式：

```java
Flux<String> flux = StreamingBridge.bridge(streamingChatModel, chatRequest);
```

### StreamingBridge.bridge(model, request, errorHandler)

带自定义错误处理的桥接：

```java
Flux<String> flux = StreamingBridge.bridge(
    streamingChatModel, 
    chatRequest,
    error -> log.error("自定义错误处理: {}", error.getMessage())
);
```

## 最佳实践

1. **推荐在 Workflow 层使用**：真正实现流式输出，节点保持简单
2. **避免在节点中阻塞**：不要在节点中使用 `.block()` 等待流式输出
3. **状态管理**：`NodeStreamingChatResponseHandler` 已处理取消和 disposed 状态
4. **错误处理**：使用带 `errorHandler` 的方法进行自定义错误处理
5. **线程调度**：`StreamingBridge` 内部已使用 `Schedulers.boundedElastic()`

## 示例代码

完整示例请参考：
- [EssayReviewApp.java](../../workflow/EssayReviewApp.java) - Workflow 层使用示例
- [ReviewEssayNodeWithBridge.java](../../nodes/essay/ReviewEssayNodeWithBridge.java) - Node 层使用示例

## 架构优势

1. **复用性**：任何节点都可以使用 `StreamingBridge` 进行流式输出
2. **一致性**：统一的流式输出处理方式
3. **可测试性**：桥接器可独立测试
4. **可维护性**：流式输出逻辑集中管理
5. **灵活性**：支持多种使用场景（Workflow 层、Node 层同步/异步）
