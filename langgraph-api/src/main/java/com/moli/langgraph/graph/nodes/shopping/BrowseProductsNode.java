package com.moli.langgraph.graph.nodes.shopping;

import com.moli.langgraph.graph.state.ShoppingState;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 浏览商品节点
 *
 * @author moli
 * @since 2026/6/8
 */
@Slf4j
public class BrowseProductsNode implements NodeAction<ShoppingState> {

    @Override
    public Map<String, Object> apply(ShoppingState state) throws Exception {
        String product = state.product();
        log.info("浏览商品: {}", product);
        TimeUnit.SECONDS.sleep(ThreadLocalRandom.current().nextLong(1, 6));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("productName", product);
        result.put("price", String.format("%.2f", ThreadLocalRandom.current().nextDouble(99, 9999)));
        result.put("shop", "官方旗舰店");
        result.put("rating", String.format("%.1f", ThreadLocalRandom.current().nextDouble(4.0, 5.0)));
        result.put("stock", ThreadLocalRandom.current().nextInt(10, 999));

        return Map.of(ShoppingState.DATA_BROWSE, result.toString());
    }
}
