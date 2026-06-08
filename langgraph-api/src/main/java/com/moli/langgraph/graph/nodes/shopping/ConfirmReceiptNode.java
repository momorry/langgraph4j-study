package com.moli.langgraph.graph.nodes.shopping;

import com.moli.langgraph.graph.state.ShoppingState;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 确认收货节点
 *
 * @author moli
 * @since 2026/6/8
 */
@Slf4j
public class ConfirmReceiptNode implements NodeAction<ShoppingState> {

    @Override
    public Map<String, Object> apply(ShoppingState state) throws Exception {
        String product = state.product();
        log.info("确认收货: {}", product);
        TimeUnit.SECONDS.sleep(ThreadLocalRandom.current().nextLong(1, 6));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("productName", product);
        result.put("orderStatus", "已完成");
        result.put("confirmTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        result.put("message", "交易成功，感谢您的购买！");

        return Map.of(ShoppingState.DATA_RECEIPT, result.toString());
    }
}
