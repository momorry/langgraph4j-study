package com.moli.langgraph.graph.state;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.Map;

/**
 * 购物流程状态
 *
 * @author moli
 * @since 2026/6/8
 */
public class ShoppingState extends AgentState {

    public static final String PRODUCT = "product";

    // 各节点数据 key
    public static final String DATA_BROWSE = "dataBrowse";
    public static final String DATA_CART = "dataCart";
    public static final String DATA_PAYMENT = "dataPayment";
    public static final String DATA_ALIPAY = "dataAlipay";
    public static final String DATA_ORDER = "dataOrder";
    public static final String DATA_LOGISTICS = "dataLogistics";
    public static final String DATA_TRANSIT = "dataTransit";
    public static final String DATA_SIGN = "dataSign";
    public static final String DATA_RECEIPT = "dataReceipt";

    /** 节点名称 → 中文描述 */
    public static final Map<String, String> NODE_LABELS = Map.ofEntries(
            Map.entry("browse_products", "浏览商品"),
            Map.entry("add_to_cart", "加入购物车"),
            Map.entry("checkout_payment", "结算支付"),
            Map.entry("alipay_deduction", "个人支付宝账户扣款"),
            Map.entry("generate_order", "生成订单"),
            Map.entry("create_logistics", "创建物流订单"),
            Map.entry("in_transit", "运输中"),
            Map.entry("sign_delivery", "签收物流"),
            Map.entry("confirm_receipt", "确认收货")
    );

    /** 节点名称 → state 中存放数据的 key */
    public static final Map<String, String> NODE_DATA = Map.ofEntries(
            Map.entry("browse_products", DATA_BROWSE),
            Map.entry("add_to_cart", DATA_CART),
            Map.entry("checkout_payment", DATA_PAYMENT),
            Map.entry("alipay_deduction", DATA_ALIPAY),
            Map.entry("generate_order", DATA_ORDER),
            Map.entry("create_logistics", DATA_LOGISTICS),
            Map.entry("in_transit", DATA_TRANSIT),
            Map.entry("sign_delivery", DATA_SIGN),
            Map.entry("confirm_receipt", DATA_RECEIPT)
    );

    public static final Map<String, Channel<?>> SCHEMA = Map.ofEntries(
            Map.entry(PRODUCT, Channels.base(() -> "")),
            Map.entry(DATA_BROWSE, Channels.base(() -> "")),
            Map.entry(DATA_CART, Channels.base(() -> "")),
            Map.entry(DATA_PAYMENT, Channels.base(() -> "")),
            Map.entry(DATA_ALIPAY, Channels.base(() -> "")),
            Map.entry(DATA_ORDER, Channels.base(() -> "")),
            Map.entry(DATA_LOGISTICS, Channels.base(() -> "")),
            Map.entry(DATA_TRANSIT, Channels.base(() -> "")),
            Map.entry(DATA_SIGN, Channels.base(() -> "")),
            Map.entry(DATA_RECEIPT, Channels.base(() -> ""))
    );

    public ShoppingState(Map<String, Object> initData) {
        super(initData);
    }

    public String product() {
        return value(PRODUCT).map(Object::toString).orElse("");
    }
}
