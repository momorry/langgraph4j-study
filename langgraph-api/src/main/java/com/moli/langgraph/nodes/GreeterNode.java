package com.moli.langgraph.nodes;

import com.moli.langgraph.state.SimpleState;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.Map;
import java.util.concurrent.TimeUnit;

// Node that adds a greeting
public class GreeterNode implements NodeAction<SimpleState> {
    @Override
    public Map<String, Object> apply(SimpleState state) {
        System.out.println("GreeterNode executing. Current messages: " + state.messages());
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return Map.of(SimpleState.MESSAGES_KEY, "Hello from GreeterNode!");
    }
}