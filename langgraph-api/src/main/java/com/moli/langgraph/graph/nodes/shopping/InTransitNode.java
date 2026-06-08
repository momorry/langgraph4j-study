package com.moli.langgraph.graph.nodes.shopping;

import com.moli.langgraph.graph.state.ShoppingState;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 运输中节点
 *
 * @author moli
 * @since 2026/6/8
 */
@Slf4j
public class InTransitNode implements NodeAction<ShoppingState> {

    @Override
    public Map<String, Object> apply(ShoppingState state) throws Exception {
        String product = state.product();
        log.info("运输中: {}", product);
        TimeUnit.SECONDS.sleep(ThreadLocalRandom.current().nextLong(1, 6));

        List<Map<String, String>> trackList = new ArrayList<>();

        Map<String, String> t1 = new LinkedHashMap<>();
        t1.put("time", "第1天");
        t1.put("desc", "包裹已从仓库发出，正在运往转运中心");
        trackList.add(t1);

        Map<String, String> t2 = new LinkedHashMap<>();
        t2.put("time", "第2天");
        t2.put("desc", "包裹到达目的地城市分拨中心");
        trackList.add(t2);

        Map<String, String> t3 = new LinkedHashMap<>();
        t3.put("time", "第3天");
        t3.put("desc", "包裹正在派送中，快递员已出发");
        trackList.add(t3);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currentStatus", "运输中");
        result.put("estimatedDays", "3天");
        result.put("tracking", trackList.toString());

        return Map.of(ShoppingState.DATA_TRANSIT, result.toString());
    }
}
