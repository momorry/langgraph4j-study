package com.moli.langgraph.graph.nodes;

import com.moli.langgraph.graph.state.SimpleState;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

// Node that adds a response
public class ResponderNode implements NodeAction<SimpleState> {
    @Override
    public Map<String, Object> apply(SimpleState state) {
        System.out.println("ResponderNode executing. Current messages: " + state.messages());
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        List<String> currentMessages = state.messages();

        if (currentMessages.contains("Hello from GreeterNode!")) {
            return Map.of(SimpleState.MESSAGES_KEY, "Acknowledged greeting!");
        }
        return Map.of(SimpleState.MESSAGES_KEY, "No greeting found.");
    }
}