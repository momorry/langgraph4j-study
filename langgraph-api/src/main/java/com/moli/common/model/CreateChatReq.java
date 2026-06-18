package com.moli.common.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 创建会话请求
 */
@Data
public class CreateChatReq {

    /**
     * 应用ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long appId;

    /**
     * 会话名称
     */
    private String chatName;

    /**
     * 模型名称
     */
    private String modelName;
}
