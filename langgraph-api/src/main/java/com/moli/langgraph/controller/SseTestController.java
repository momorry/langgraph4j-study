package com.moli.langgraph.controller;

import com.moli.langgraph.graph.NodeDetail;
import com.moli.langgraph.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * SSE 纯 For 循环测试控制器
 * <p>
 * 不依赖 langgraph4j，仅用纯 for 循环 + sleep 模拟节点执行。
 * <p>
 * 使用 SseEmitter 而非 Flux&lt;ServerSentEvent&gt;，
 * 因为项目是 Spring MVC (Tomcat)，Flux 的 ReactiveTypeHandler
 * 不保证每个事件 flush，会导致事件缓冲一次性输出。
 * SseEmitter.send() + emitter.flush() 是 Spring MVC 下唯一可靠的逐帧推送方式。
 *
 * @author moli
 * @since 2026/6/8
 */
@Slf4j
@RestController
public class SseTestController {

    private static final List<String> NODE_NAMES = List.of(
            "节点A-浏览", "节点B-加购", "节点C-支付", "节点D-发货", "节点E-收货"
    );

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @PostMapping("/sse-test")
    public SseEmitter sseTest() {
        log.info("SSE测试开始");
        // timeout = 0 表示永不超时
        SseEmitter emitter = new SseEmitter(0L);

        executor.execute(() -> {
            try {
                for (String name : NODE_NAMES) {
                    // 发送 running
                    emitNode(emitter, name, "running", "处理中...", "");

                    // 模拟耗时 2 秒
                    TimeUnit.SECONDS.sleep(2);

                    // 发送 done
                    emitNode(emitter, name, "done", "", "{\"result\":\"success\"}");
                }

                // 完成事件
                emitter.send(SseEmitter.event().data("{\"type\":\"done\"}"));
                emitter.complete();
                log.info("SSE测试完成");

            } catch (Exception e) {
                log.error("SSE测试失败", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private void emitNode(SseEmitter emitter, String name, String status,
                          String message, String data) throws Exception {
        NodeDetail detail = new NodeDetail();
        detail.setName(name);
        detail.setStatus(status);
        detail.setMessage(message);
        detail.setData(data);
        detail.setCostTime("2.00秒");

        emitter.send(SseEmitter.event().data(JsonUtil.obj2String(detail)));
        // 关键：手动 flush，确保事件立即推送到客户端
    }
}
