# LangGraph 真正的流式输出实现指南

## ⚠️ 重要发现

**LangGraph4j 的 AsyncGenerator 会缓存输出，无法实现真正的流式！**

详见：[STREAMING_TRUTH.md](./STREAMING_TRUTH.md)

## ✅ 正确的实现方式（2024-05-21 更新）

### 核心原则

1. **图内执行前置节点**：保持工作流完整性
2. **图外桥接真正的流式**：使用 `StreamingChatResponseHandler`
3. **立即推送 chunk**：LLM 返回一个 token 就立即 `sink.next(token)`

## ⚠️ 错误实现方式

### 错误 1：在图外调用 LLM（MarketReportApp 的方式）

```java
// ❌ 错误：在图外直接调用 streamingChatModel
MarketReportState state = runPreprocessGraph(graph, initState);

// 绕过了 LangGraph 的流式机制
streamingChatModel.chat(request, new StreamingChatResponseHandler() {
    @Override
    public void onPartialResponse(String partialResponse) {
        sink.next(partialResponse);
    }
});
```

**问题**：
- 破坏了 LangGraph 的工作流完整性
- 无法处理复杂的多出口、条件分支场景
- 违背了 LangGraph 自包含工作流的设计初衷

### 错误 2：使用 graph.stream() 但有缓存（MarketReportAppV1 的方式）

```java
// ❌ 错误：虽然使用了 graph.stream()，但内部有缓存
for (NodeOutput<MarketReportState> output : graph.stream(initState)) {
    if (output instanceof StreamingOutput<?> streaming) {
        String chunk = streaming.chunk();
        sink.next(chunk);  // 实际上会在 1 毫秒内收到所有数据
    }
}
```

**问题**：
- 后端缓存了所有 chunk
- 前端在 1 毫秒内一次性收到所有数据
- 没有真正的打字机效果
- 需要在前端模拟打字机效果（这是错误的）

## ✅ 正确实现方式

### 核心原则

1. **流式节点必须在图内**：使用 `StreamingChatGenerator` 并返回 `LangGraphStreaming.streamingMessages(generator)`
2. **直接遍历 AsyncGenerator**：使用 `AsyncGenerator<NodeOutput<State>>` 遍历
3. **立即推送 chunk**：LLM 返回一个 token 就立即推送到前端

### 正确的节点实现（ReviewEssayNode）

```java
@Slf4j
@RequiredArgsConstructor
public class ReviewEssayNode implements NodeAction<EssayReviewState> {

    public static final String REVIEW_ESSAY = "review_essay";
    private final StreamingChatModel streamingChatModel;

    @Override
    public Map<String, Object> apply(EssayReviewState state) {
        // 1. 构建请求
        ChatRequest chatRequest = buildReviewRequest(state);

        // 2. 创建流式生成器（关键）
        var generator = StreamingChatGenerator.<EssayReviewState>builder()
                .mapResult(response -> {
                    String fullComment = response.aiMessage().text();
                    String grade = extractGrade(fullComment);
                    return Map.of(
                            EssayReviewState.TEACHER_COMMENT_KEY, fullComment,
                            EssayReviewState.ESSAY_GRADE_KEY, grade
                    );
                })
                .startingNode(REVIEW_ESSAY)
                .startingState(state)
                .build();

        // 3. 调用流式聊天模型
        streamingChatModel.chat(chatRequest, generator.handler());
        
        // 4. 返回流式消息（关键：使用 _streaming_messages 键）
        return LangGraphStreaming.streamingMessages(generator);
    }
}
```

### 正确的工作流执行（EssayReviewApp）

```java
public Flux<String> reviewEssay(String studentName, String essayTopic) {
    try {
        CompiledGraph<EssayReviewState> graph = buildGraph();
        Map<String, Object> initState = buildInitState(studentName, essayTopic);

        return Flux.<String>create(sink -> {
            try {
                // ✅ 正确：直接遍历 AsyncGenerator
                AsyncGenerator<NodeOutput<EssayReviewState>> stream = graph.stream(initState);
                
                for (NodeOutput<EssayReviewState> output : stream) {
                    // ✅ 正确：检查是否为流式输出节点
                    if (output instanceof StreamingOutput<EssayReviewState> streamingOutput
                            && ReviewEssayNode.REVIEW_ESSAY.equals(streamingOutput.node())) {
                        
                        // ✅ 正确：立即推送 chunk（真正的流式）
                        String chunk = streamingOutput.chunk();
                        if (chunk != null && !chunk.isEmpty()) {
                            log.debug("emit chunk immediately: {}", chunk);
                            sink.next(chunk);  // LLM 返回一个就推送一个
                        }
                    }
                }
                
                sink.complete();
            } catch (Exception e) {
                sink.error(e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
        
    } catch (GraphStateException e) {
        return Flux.error(e);
    }
}
```

## 🔑 关键要点

### 1. 节点内使用 StreamingChatGenerator

```java
// ✅ 正确
var generator = StreamingChatGenerator.<State>builder()
        .mapResult(response -> Map.of("answer", response.aiMessage().text()))
        .startingNode(NODE_NAME)
        .startingState(state)
        .build();

streamingChatModel.chat(request, generator.handler());
return LangGraphStreaming.streamingMessages(generator);
```

### 2. 使用 AsyncGenerator 遍历

```java
// ✅ 正确
AsyncGenerator<NodeOutput<State>> stream = graph.stream(initState);

for (NodeOutput<State> output : stream) {
    if (output instanceof StreamingOutput<State> streamingOutput) {
        String chunk = streamingOutput.chunk();
        sink.next(chunk);  // 立即推送
    }
}
```

### 3. 不要使用缓存或队列

```java
// ❌ 错误：不要缓存 chunk
List<String> chunks = new ArrayList<>();
for (NodeOutput<State> output : stream) {
    if (output instanceof StreamingOutput<State> streaming) {
        chunks.add(streaming.chunk());  // 缓存会导致延迟
    }
}

// ❌ 错误：不要使用线程池延迟推送
Flux.interval(Duration.ofMillis(100))
    .map(i -> chunks.get(i))
    .subscribe(sink::next);
```

## 📊 数据流对比

### 错误方式（有缓存）

```
LLM 生成 token1 → 缓存到队列
LLM 生成 token2 → 缓存到队列
LLM 生成 token3 → 缓存到队列
...
LLM 生成 tokenN → 缓存到队列
                    ↓
              一次性刷出所有 token（1毫秒内）
                    ↓
              前端收到完整文本
                    ↓
              前端模拟打字机效果（错误！）
```

### 正确方式（真正的流式）

```
LLM 生成 token1 → 立即推送 → 前端显示 token1
LLM 生成 token2 → 立即推送 → 前端显示 token2
LLM 生成 token3 → 立即推送 → 前端显示 token3
...
LLM 生成 tokenN → 立即推送 → 前端显示 tokenN
                    ↓
              真正的打字机效果（后端实现）
```

## 🎯 验证方法

### 1. 后端日志检查

正确的流式输出应该看到：

```log
DEBUG emit chunk immediately: 这
DEBUG emit chunk immediately: 篇
DEBUG emit chunk immediately: 作
DEBUG emit chunk immediately: 文
DEBUG emit chunk immediately: 写
DEBUG emit chunk immediately: 得
```

每个 chunk 都有不同的时间戳，间隔几毫秒到几十毫秒。

### 2. 前端 Network 检查

打开浏览器 DevTools → Network：

**正确的流式输出**：
- 请求持续时间长（5-15 秒）
- Response 面板中看到文字逐个出现
- Transfer Size 逐渐增加

**错误的缓存输出**：
- 请求持续时间长（5-15 秒）
- Response 面板中前 4.9 秒无内容
- 最后 0.1 秒突然显示全部文字

### 3. 前端代码检查

```typescript
// ✅ 正确：直接显示接收到的数据
onEvent: (data) => {
  assistant.content = data  // data 是累积的完整文本
}

// ❌ 错误：前端模拟打字机效果
onEvent: (data) => {
  // 不要这样做！
  simulateTypingEffect(data)
}
```

## 🔧 完整示例

### 节点代码

```java
@Slf4j
@RequiredArgsConstructor
public class StreamNode implements NodeAction<MyState> {
    
    private final StreamingChatModel streamingChatModel;
    
    @Override
    public Map<String, Object> apply(MyState state) {
        ChatRequest request = buildRequest(state);
        
        var generator = StreamingChatGenerator.<MyState>builder()
                .mapResult(response -> Map.of("answer", response.aiMessage().text()))
                .startingNode("stream_node")
                .startingState(state)
                .build();
        
        streamingChatModel.chat(request, generator.handler());
        return LangGraphStreaming.streamingMessages(generator);
    }
}
```

### 工作流代码

```java
public Flux<String> executeWorkflow(String input) {
    CompiledGraph<MyState> graph = buildGraph();
    Map<String, Object> initState = Map.of("input", input);
    
    return Flux.<String>create(sink -> {
        try {
            AsyncGenerator<NodeOutput<MyState>> stream = graph.stream(initState);
            
            for (NodeOutput<MyState> output : stream) {
                if (output instanceof StreamingOutput<MyState> streaming) {
                    String chunk = streaming.chunk();
                    if (chunk != null && !chunk.isEmpty()) {
                        sink.next(chunk);  // 立即推送
                    }
                }
            }
            
            sink.complete();
        } catch (Exception e) {
            sink.error(e);
        }
    }).subscribeOn(Schedulers.boundedElastic());
}
```

## 📝 总结

| 特性 | 错误方式 | 正确方式 |
|------|---------|---------|
| 流式节点位置 | 图外或图内 | 必须在图内 |
| 生成器类型 | 直接调用 LLM | StreamingChatGenerator |
| 返回方式 | 直接调用 Handler | LangGraphStreaming.streamingMessages() |
| 遍历方式 | 普通 for 循环 | AsyncGenerator |
| Chunk 处理 | 缓存后批量推送 | 立即推送 |
| 打字机效果 | 前端模拟 | 后端真正实现 |
| 响应延迟 | 高（缓存导致） | 低（实时推送） |

## ⚡ 性能对比

### 错误方式（缓存）
- 首字延迟：5-15 秒（等待 LLM 完成）
- 总耗时：5-15 秒
- 用户体验：❌ 长时间等待，突然显示全部

### 正确方式（真正流式）
- 首字延迟：0.5-2 秒（第一个 token 到达）
- 总耗时：5-15 秒
- 用户体验：✅ 立即看到文字逐个出现

## 🎉 最佳实践

1. **永远在节点内使用 StreamingChatGenerator**
2. **永远使用 AsyncGenerator 遍历 graph.stream()**
3. **永远立即推送 chunk，不要缓存**
4. **永远在后端实现打字机效果，不要在前端模拟**
5. **永远保持 LangGraph 工作流的完整性**

遵循这些原则，你就能实现真正的流式输出，给用户带来优秀的打字机体验！✨
