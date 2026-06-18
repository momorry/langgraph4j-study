package com.moli.common.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Map;

@Data
public class StreamChatReq {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long appId;

    private String modelName;

    private String question;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long chatId;

    private String messageId;



    private Map<String, Object> inputs;
}
