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
 * 创建物流订单节点
 *
 * @author moli
 * @since 2026/6/8
 */
@Slf4j
public class CreateLogisticsNode implements NodeAction<ShoppingState> {

    private static final String[] COMPANIES = {"顺丰速运", "京东物流", "中通快递", "圆通速递", "韵达快递"};

    @Override
    public Map<String, Object> apply(ShoppingState state) throws Exception {
        String product = state.product();
        log.info("创建物流订单: {}", product);
        TimeUnit.SECONDS.sleep(ThreadLocalRandom.current().nextLong(1, 6));

        String company = COMPANIES[ThreadLocalRandom.current().nextInt(COMPANIES.length)];
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("trackingNo", "SF" + System.currentTimeMillis());
        result.put("company", company);
        result.put("sender", "官方旗舰店仓库");
        result.put("receiver", "用户收货地址");
        result.put("status", "已揽件");
        result.put("createTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        return Map.of(ShoppingState.DATA_LOGISTICS, result.toString());
    }
}
