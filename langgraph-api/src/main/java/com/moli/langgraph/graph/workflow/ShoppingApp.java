package com.moli.langgraph.graph.workflow;

import com.moli.langgraph.graph.NodeDetail;
import com.moli.langgraph.graph.graph.ShoppingGraph;
import com.moli.langgraph.graph.state.ShoppingState;
import com.moli.langgraph.model.ShoppingReq;
import com.moli.langgraph.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.NodeOutput;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

/**
 * 购物流程应用服务
 * <p>
 * 通过 langgraph4j 构建完整的购物工作流，逐节点发送 SSE 进度事件。
 *
 * @author moli
 * @since 2026/6/8
 */
@Slf4j
@Service
public class ShoppingApp {

    /** 购物流程节点执行顺序 */
    private static final List<String> NODE_ORDER = List.of(
            "browse_products", "add_to_cart", "checkout_payment", "alipay_deduction",
            "generate_order", "create_logistics", "in_transit", "sign_delivery", "confirm_receipt"
    );

    public Flux<ServerSentEvent<String>> shopping(ShoppingReq shoppingReq) {
        ShoppingGraph graph = new ShoppingGraph();

        return Flux.<ServerSentEvent<String>>create(sink -> {
            try {
                CompiledGraph<ShoppingState> compiledGraph = graph.buildGraph();
                Map<String, Object> initState = graph.buildInitState(shoppingReq.getProduct());

                // ===== 在图执行前，先发送第一个节点的 running 事件 =====
                // 因为 compiledGraph.stream() 的迭代器是阻塞的：
                //   next() 会阻塞到当前节点执行完毕才返回
                // 所以如果在 for 循环内才发 running，前端看到的 running/done 几乎是同时到达
                // 解决：提前发送 running，让前端在节点真正执行期间就能看到 "执行中" 状态
                emitRunning(NODE_ORDER.get(0), sink);

                long start = System.currentTimeMillis();
                long end;
                int nodeIndex = 0;

                for (var output : compiledGraph.stream(initState)) {
                    if (output instanceof NodeOutput<?> nodeOutput) {
                        end = System.currentTimeMillis();
                        ShoppingState state = (ShoppingState) nodeOutput.state();
                        String node = output.node();

                        // 发送当前节点的 done 事件
                        NodeDetail nodeDetail = new NodeDetail();
                        nodeDetail.setName(ShoppingState.NODE_LABELS.get(node));
                        nodeDetail.setMessage("");
                        nodeDetail.setData(JsonUtil.obj2String(state.data().get(ShoppingState.NODE_DATA.get(node))));
                        nodeDetail.setStatus("done");
                        nodeDetail.setCostTime(String.format("%.2f秒", (end - start) / 1000.0));
                        start = end;

                        sink.next(ServerSentEvent.<String>builder().data(JsonUtil.obj2String(nodeDetail)).build());

                        // 提前发送下一个节点的 running 事件（如果有）
                        nodeIndex++;
                        if (nodeIndex < NODE_ORDER.size()) {
                            emitRunning(NODE_ORDER.get(nodeIndex), sink);
                        }
                    }
                }

                // 发送完成事件
                sink.next(ServerSentEvent.<String>builder()
                        .data(JsonUtil.obj2String(Map.of("type", "done")))
                        .build());
                sink.complete();

            } catch (Exception e) {
                log.error("购物流程执行失败", e);
                sink.error(e);
            }
        }, FluxSink.OverflowStrategy.IGNORE).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 发送节点的 running 状态事件
     */
    private void emitRunning(String nodeKey, FluxSink<ServerSentEvent<String>> sink) {
        NodeDetail detail = new NodeDetail();
        detail.setName(ShoppingState.NODE_LABELS.get(nodeKey));
        detail.setMessage("处理中...");
        detail.setData("");
        detail.setStatus("running");
        detail.setCostTime("");
        sink.next(ServerSentEvent.<String>builder().data(JsonUtil.obj2String(detail)).build());
    }
}
