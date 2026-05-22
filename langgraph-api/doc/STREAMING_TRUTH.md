# LangGraph4j 流式输出的真相

## ⚠️ 重要发现

**LangGraph4j 的 AsyncGenerator 不支持真正的流式输出！**

## 🔍 问题现象

### 后端日志
```
13:25:01.794 chunk: 亲爱的同学
13:25:01.882 chunk: 张三
13:25:01.934 chunk: 你好！
...
13:25:10.940 chunk: 5日
```
**耗时：约 9 秒**（从 13:25:01 到 13:25:10）

### 前端日志
```
13:25:10.823 message: 是一次心灵的
13:25:10.823 message: 散步，一次对
13:25:10.825 message: 生命意义的温柔
...
13:25:10.940 message: 5日
```
**耗时：不到 0.2 秒**（所有消息都在 13:25:10.823 ~ 13:25:10.940 之间）

### 结论
- 后端花了 9 秒生成所有 chunk
- 前端在 0.2 秒内全部收到
- **chunk 被缓存在后端，最后一次性刷出！**

## 🔬 根本原因

### 原因 1：AsyncGenerator 的阻塞特性

```java
// ❌ 错误：AsyncGenerator 是阻塞的
AsyncGenerator<NodeOutput<State>> stream = graph.stream(initState);

for (NodeOutput<State> output : stream) {
    // 这个 for 循环会等所有 chunk 都生成完毕才继续
    // 因为 AsyncGenerator 内部使用了 BlockingQueue
    if (output instanceof StreamingOutput<State> streaming) {
        sink.next(streaming.chunk());  // 虽然立即调用，但上层在缓存
    }
}
```

**问题**：
- `AsyncGenerator` 底层使用 `BlockingQueue`
- `graph.stream()` 返回的生成器会等节点执行完毕才返回
- 即使节点内部是流式的，外层循环也会缓存所有输出

### 原因 2：Reactor Flux.create 的默认策略

```java
// ❌ 错误：Flux.create 默认使用 OverflowStrategy.BUFFER
return Flux.<String>create(sink -> {
    for (...) {
        sink.next(chunk);  // 会被 Reactor 缓存
    }
}).subscribeOn(Schedulers.boundedElastic());
```

**问题**：
- `Flux.create` 默认使用 `BUFFER` 策略
- 会缓存所有元素直到订阅者准备好
- 即使后端立即调用 `sink.next()`，Reactor 也会缓存

## ✅ 正确的实现方式

### 方案：图内执行前置节点 + 图外桥接真正的流式

```java
public Flux<String> reviewEssay(String studentName, String essayTopic) {
    try {
        CompiledGraph<EssayReviewState> graph = buildGraph();
        Map<String, Object> initState = buildInitState(studentName, essayTopic);

        return Flux.<String>create(sink -> {
            try {
                // 第一步：执行图的前置节点（布置作业、提交作文）
                EssayReviewState state = runPreprocessGraph(graph, initState);
                
                // 第二步：直接在图外桥接 StreamingChatModel
                AtomicBoolean cancelled = new AtomicBoolean(false);
                AtomicBoolean streamed = new AtomicBoolean(false);
                sink.onCancel(() -> cancelled.set(true));
                sink.onDispose(() -> cancelled.set(true));

                // 构建批改请求
                var chatRequest = ReviewEssayNode.buildReviewRequest(state);
                
                // 直接调用流式聊天模型，每个 chunk 立即推送
                streamingChatModel.chat(chatRequest, new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        if (cancelled.get() || StringUtils.isEmpty(partialResponse)) {
                            return;
                        }
                        streamed.set(true);
                        log.debug("emit chunk at {}", LocalDateTime.now());
                        sink.next(partialResponse);  // ✅ 真正的流式推送
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse completeResponse) {
                        if (!cancelled.get()) {
                            sink.complete();
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        if (!cancelled.get()) {
                            sink.error(error);
                        }
                    }
                });
            } catch (Exception e) {
                sink.error(e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
        
    } catch (GraphStateException e) {
        return Flux.error(e);
    }
}

/**
 * 执行图的前置节点（不包含流式输出节点）
 */
private EssayReviewState runPreprocessGraph(
        CompiledGraph<EssayReviewState> graph, 
        Map<String, Object> initState) throws Exception {
    EssayReviewState latestState = null;
    for (var output : graph.stream(initState)) {
        if (output instanceof NodeOutput<?> nodeOutput) {
            latestState = (EssayReviewState) nodeOutput.state();
        }
    }
    return latestState;
}
```

## 📊 数据流对比

### 错误方式（使用 AsyncGenerator）

```
LLM 生成 token1 → AsyncGenerator 队列
LLM 生成 token2 → AsyncGenerator 队列
LLM 生成 token3 → AsyncGenerator 队列
...
LLM 生成 tokenN → AsyncGenerator 队列
                    ↓
            graph.stream() 返回
                    ↓
            for 循环遍历所有输出
                    ↓
            Flux.create 缓存所有 chunk
                    ↓
            一次性推送给前端（1毫秒内）
```

### 正确方式（图外桥接）

```
前置节点执行（布置作业、提交作文）
                    ↓
            获取最终 state
                    ↓
      直接在图外调用 streamingChatModel
                    ↓
LLM 生成 token1 → onPartialResponse → sink.next(token1) → 前端立即显示
LLM 生成 token2 → onPartialResponse → sink.next(token2) → 前端立即显示
LLM 生成 token3 → onPartialResponse → sink.next(token3) → 前端立即显示
...
LLM 生成 tokenN → onPartialResponse → sink.next(tokenN) → 前端立即显示
                    ↓
            真正的打字机效果！
```

## 🎯 关键要点

### 1. AsyncGenerator 不适合真正的流式

```java
// ❌ 不适合真正的流式
AsyncGenerator<NodeOutput<State>> stream = graph.stream(initState);
for (NodeOutput<State> output : stream) {
    // 这里拿到的 chunk 已经被缓存了
}

// ✅ 适合真正的流式
streamingChatModel.chat(request, new StreamingChatResponseHandler() {
    @Override
    public void onPartialResponse(String partialResponse) {
        sink.next(partialResponse);  // 立即推送
    }
});
```

### 2. 图内节点 vs 图外桥接

| 场景 | 方案 | 原因 |
|------|------|------|
| 非流式节点 | 图内执行 | 保持工作流完整性 |
| 流式节点（需要真正的流式） | 图外桥接 | AsyncGenerator 会缓存 |
| 混合场景 | 图内前置 + 图外流式 | 兼顾完整性和性能 |

### 3. Reactor Flux 策略

```java
// ❌ 默认 BUFFER 策略（会缓存）
Flux.<String>create(sink -> { ... })

// ✅ 使用 push + LATEST 策略（不缓存）
Flux.<String>push(sink -> { ... }, FluxSink.OverflowStrategy.LATEST)

// ✅ 或者直接桥接 StreamingChatResponseHandler（最佳）
streamingChatModel.chat(request, new StreamingChatResponseHandler() { ... })
```

## 💡 为什么 MarketReportApp 是对的

虽然 MarketReportApp 将流式输出放在图外，破坏了 LangGraph 的完整性，但它**至少实现了真正的流式输出**。

```java
// MarketReportApp 的实现
MarketReportState state = runPreprocessGraph(graph, initState);

// 直接在图外调用流式模型
streamingChatModel.chat(request, new StreamingChatResponseHandler() {
    @Override
    public void onPartialResponse(String partialResponse) {
        sink.next(partialResponse);  // ✅ 真正的流式
    }
});
```

**这就是为什么 MarketReportApp 的前端能看到打字机效果，而我们之前的实现不能！**

## 📝 最佳实践

### 推荐的架构

```
┌─────────────────────────────────────┐
│         LangGraph 图内               │
│  ┌─────────┐                        │
│  │ START   │                        │
│  └────┬────┘                        │
│       │                             │
│  ┌────▼────┐                        │
│  │ Node 1  │  前置节点（非流式）    │
│  └────┬────┘                        │
│       │                             │
│  ┌────▼────┐                        │
│  │ Node 2  │  前置节点（非流式）    │
│  └────┬────┘                        │
│       │                             │
│  ┌────▼────┐                        │
│  │  END    │  返回 state            │
│  └─────────┘                        │
└─────────────────────────────────────┘
                    ↓
            获取最终 state
                    ↓
┌─────────────────────────────────────┐
│         图外桥接（真正的流式）       │
│                                     │
│  streamingChatModel.chat(request,   │
│      new StreamingChatResponseHandler() {
│          onPartialResponse(token) { │
│              sink.next(token)  ✅   │
│          }                          │
│      })                             │
└─────────────────────────────────────┘
```

### 代码模板

```java
public Flux<String> executeWithStreaming(Input input) {
    // 1. 构建图（只包含非流式节点）
    CompiledGraph<State> graph = buildGraph();
    Map<String, Object> initState = buildInitState(input);

    return Flux.<String>create(sink -> {
        try {
            // 2. 执行图的前置节点
            State state = runGraph(graph, initState);
            
            // 3. 图外桥接真正的流式
            streamingChatModel.chat(
                buildRequest(state),
                new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String token) {
                        sink.next(token);  // 真正的流式
                    }
                    
                    @Override
                    public void onCompleteResponse(ChatResponse response) {
                        sink.complete();
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        sink.error(error);
                    }
                }
            );
        } catch (Exception e) {
            sink.error(e);
        }
    }).subscribeOn(Schedulers.boundedElastic());
}
```

## 🎉 总结

1. **LangGraph4j 的 AsyncGenerator 会缓存输出**，不适合真正的流式场景
2. **必须在图外桥接 StreamingChatModel**，才能实现真正的打字机效果
3. **这不是 LangGraph 的设计问题**，而是 Java 异步生成器的限制
4. **正确的做法**：图内执行前置节点 + 图外桥接真正的流式

虽然这破坏了 LangGraph 的完整性，但**这是目前唯一能实现真正流式输出的方式**。

## 📚 参考

- [MarketReportApp.java](./src/main/java/com/moli/langgraph/workflow/MarketReportApp.java) - 正确的流式实现参考
- [EssayReviewApp.java](./src/main/java/com/moli/langgraph/workflow/EssayReviewApp.java) - 本次修复的实现
- [Reactor FluxSink 文档](https://projectreactor.io/docs/core/release/api/reactor/core/publisher/FluxSink.html)
