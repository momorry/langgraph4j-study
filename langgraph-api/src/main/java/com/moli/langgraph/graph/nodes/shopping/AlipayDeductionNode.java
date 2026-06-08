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
 * 个人支付宝账户扣款节点
 *
 * @author moli
 * @since 2026/6/8
 */
@Slf4j
public class AlipayDeductionNode implements NodeAction<ShoppingState> {

    @Override
    public Map<String, Object> apply(ShoppingState state) throws Exception {
        String product = state.product();
        log.info("支付宝扣款: {}", product);
        TimeUnit.SECONDS.sleep(ThreadLocalRandom.current().nextLong(1, 6));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("alipayAccount", "138****8888");
        result.put("deductAmount", String.format("%.2f", ThreadLocalRandom.current().nextDouble(99, 9999)));
        result.put("balance", String.format("%.2f", ThreadLocalRandom.current().nextDouble(1000, 50000)));
        result.put("transactionId", "ALI" + System.currentTimeMillis());
        result.put("deductTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        result.put("status", "扣款成功");

        return Map.of(ShoppingState.DATA_ALIPAY, result.toString());
    }
}
