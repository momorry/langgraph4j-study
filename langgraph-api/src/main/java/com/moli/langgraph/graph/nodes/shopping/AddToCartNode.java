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
 * 加入购物车节点
 *
 * @author moli
 * @since 2026/6/8
 */
@Slf4j
public class AddToCartNode implements NodeAction<ShoppingState> {

    @Override
    public Map<String, Object> apply(ShoppingState state) throws Exception {
        String product = state.product();
        log.info("加入购物车: {}", product);
        TimeUnit.SECONDS.sleep(ThreadLocalRandom.current().nextLong(1, 6));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("productName", product);
        result.put("quantity", 1);
        result.put("cartId", "CART" + System.currentTimeMillis());
        result.put("addTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        return Map.of(ShoppingState.DATA_CART, result.toString());
    }
}
