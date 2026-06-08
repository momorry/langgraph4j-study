package com.moli.langgraph.graph;

import lombok.Data;

/**
 * @author moli
 * @since 2026/6/8 13:43
 */
@Data
public class NodeDetail {
    private String name;
    private String message;
    private String data;
    private String costTime;

    /**
     * running
     * done
     */
    private String status;

}
