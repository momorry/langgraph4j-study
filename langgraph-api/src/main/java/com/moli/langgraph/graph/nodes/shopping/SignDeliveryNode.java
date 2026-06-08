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
 * 签收物流节点
 *
 * @author moli
 * @since 2026/6/8
 */
@Slf4j
public class SignDeliveryNode implements NodeAction<ShoppingState> {

    @Override
    public Map<String, Object> apply(ShoppingState state) throws Exception {
        String product = state.product();
        log.info("签收物流: {}", product);
        TimeUnit.SECONDS.sleep(ThreadLocalRandom.current().nextLong(1, 6));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("signType", "本人签收");
        result.put("signer", "用户本人");
        result.put("status", "已签收");
        result.put("signTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        return Map.of(ShoppingState.DATA_SIGN, result.toString());
    }
}
