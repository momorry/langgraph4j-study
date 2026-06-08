package com.moli.langgraph.model;

import lombok.Data;

/**
 * 购物流程请求
 *
 * @author moli
 * @since 2026/6/8
 */
@Data
public class ShoppingReq {

    /** 用户输入，例如："我要买一个手机" */
    private String product;
}
