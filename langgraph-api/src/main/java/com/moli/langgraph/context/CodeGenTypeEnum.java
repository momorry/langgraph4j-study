package com.moli.langgraph.context;

import lombok.Getter;

/**
 *
 * @author likethewind
 * @since 2026/5/15 16:28
 *
 */
@Getter
public enum CodeGenTypeEnum {
    HTML("");
    private final String text;
     CodeGenTypeEnum(String text) {
        this.text = text;
    }


}
