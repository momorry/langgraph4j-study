package com.moli.langgraph.graph.graph;

import com.moli.langgraph.graph.nodes.shopping.*;
import com.moli.langgraph.graph.state.ShoppingState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;

import java.util.HashMap;
import java.util.Map;

import static org.bsc.langgraph4j.GraphDefinition.END;
import static org.bsc.langgraph4j.GraphDefinition.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 购物流程工作流图
 * <p>
 * 图结构：
 * START → browse_products → add_to_cart → checkout_payment → alipay_deduction
 *       → generate_order → create_logistics → in_transit → sign_delivery
 *       → confirm_receipt → END
 *
 * @author moli
 * @since 2026/6/8
 */
@Data
@Slf4j
public class ShoppingGraph {

    /**
     * 构建购物工作流图
     */
    public CompiledGraph<ShoppingState> buildGraph() throws GraphStateException {
        return new StateGraph<>(ShoppingState.SCHEMA, ShoppingState::new)
                // 添加节点
                .addNode("browse_products", node_async(new BrowseProductsNode()))
                .addNode("add_to_cart", node_async(new AddToCartNode()))
                .addNode("checkout_payment", node_async(new CheckoutPaymentNode()))
                .addNode("alipay_deduction", node_async(new AlipayDeductionNode()))
                .addNode("generate_order", node_async(new GenerateOrderNode()))
                .addNode("create_logistics", node_async(new CreateLogisticsNode()))
                .addNode("in_transit", node_async(new InTransitNode()))
                .addNode("sign_delivery", node_async(new SignDeliveryNode()))
                .addNode("confirm_receipt", node_async(new ConfirmReceiptNode()))

                // 添加边：线性流程
                .addEdge(START, "browse_products")
                .addEdge("browse_products", "add_to_cart")
                .addEdge("add_to_cart", "checkout_payment")
                .addEdge("checkout_payment", "alipay_deduction")
                .addEdge("alipay_deduction", "generate_order")
                .addEdge("generate_order", "create_logistics")
                .addEdge("create_logistics", "in_transit")
                .addEdge("in_transit", "sign_delivery")
                .addEdge("sign_delivery", "confirm_receipt")
                .addEdge("confirm_receipt", END)

                // 编译图
                .compile();
    }

    /**
     * 构建初始状态
     */
    public Map<String, Object> buildInitState(String product) {
        Map<String, Object> state = new HashMap<>();
        state.put(ShoppingState.PRODUCT, product);
        return state;
    }
}
