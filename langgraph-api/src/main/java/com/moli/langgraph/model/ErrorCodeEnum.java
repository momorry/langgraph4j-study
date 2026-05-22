package com.moli.langgraph.model;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 系统异常类型
 *
 * @author Wilson
 * @date 2021-06-23 4:30 下午
 **/
@AllArgsConstructor
@Getter
public enum ErrorCodeEnum {

    /**
     * 成功
     */
    SUCCESS(200,"成功"),
    /**
     * 一般的失败
     */
    FAIL(500,"失败"),

    /**
     * 失败，并且返回自定义数据，调用方无须做全局化异常处理
     */
    FAIL_DATA(501,"失败"),

    /**
     * 失败，当查找不到资源时，调用方需要做定制化全局异常处理
     */
    RESOURCE_NOT_FOUND(4041, "资源不存在");

    /**
     * 如需自定义代码，请自行追加
     */

    private Integer errorCode;

    private String errorDesc;

    public static ErrorCodeEnum getEnumByErrorCode(Integer errorCode) {
        for (ErrorCodeEnum errorEnum : ErrorCodeEnum.values()) {
            if (errorEnum.errorCode.equals(errorCode)) {
                return errorEnum;
            }
        }
        return null;
    }

    public static String getMsgByErrorCode(Integer errorCode) {
        for (ErrorCodeEnum errorEnum : ErrorCodeEnum.values()) {
            if (errorEnum.errorCode.equals(errorCode)) {
                return errorEnum.getErrorDesc();
            }
        }
        return null;
    }

    public static void throwIfNotExists(Integer errorCode) {
        boolean exist = false;
        for (ErrorCodeEnum v : ErrorCodeEnum.values()) {
            if (v.getErrorCode().equals(errorCode)) {
                exist = true;
                break;
            }
        }
        if (!exist) {
            throw new IllegalArgumentException("请填写ErrorCodeEnum枚举的errorCode");
        }
    }

}

