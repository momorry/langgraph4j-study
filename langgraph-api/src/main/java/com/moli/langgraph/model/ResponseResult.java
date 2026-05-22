package com.moli.langgraph.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 响应信息主体
 * @author  moli
 * @since 2021/6/30 10:37 上午
 */
@Data
public class ResponseResult<T> implements Serializable {

    private static final long serialVersionUID = -3197059789017703937L;

    /**
     * 200 成功 非200错误
     */
    private Integer code;
    private String msg;

    private T data;

    /**
     * 当失败时code=501，如果需要返回某些数据
     */
    private Object data4Fail;

    public boolean isSuccess() {
        return this.code==200;
    }
    /***
     * 构建ResponseResult 对象 - （有返回内容）
     *
     * @param errorCodeEnum 状态码
     * @param data 返回内容
     * @return
     */
    private static <T> ResponseResult<T> response(T data, ErrorCodeEnum errorCodeEnum) {
        ResponseResult<T> apiResult = new ResponseResult<>();
        apiResult.setCode(errorCodeEnum.getErrorCode());
        apiResult.setData(data);
        apiResult.setMsg(errorCodeEnum.getErrorDesc());
        return apiResult;
    }

    /***
     * 构建ResponseResult 对象 - （有返回内容）
     *
     * @param code 状态码
     * @param msg  返回信息
     * @param data 返回内容
     * @return
     */
    private static <T> ResponseResult<T> response(T data, Integer code, String msg) {
        ErrorCodeEnum.throwIfNotExists(code);
        ResponseResult<T> apiResult = new ResponseResult<>();
        apiResult.setCode(code);
        apiResult.setData(data);
        apiResult.setMsg(msg);
        return apiResult;
    }

    /***
     * 构建ResponseResult 对象 - （返回内容）
     *
     * @param code 状态码
     * @param msg  返回信息
     * @return
     */
    private static <T> ResponseResult<T> response(Integer code, String msg) {
        ResponseResult<T> apiResult = new ResponseResult<>();
        apiResult.setCode(code);
        apiResult.setMsg(msg);
        return apiResult;
    }


    public static <T> ResponseResult<T> ok() {
        return response(null, ErrorCodeEnum.SUCCESS);
    }
    public static <T> ResponseResult<T> ok(T data) {
        return response(data, ErrorCodeEnum.SUCCESS);
    }

    public static <T> ResponseResult<T> ok(T data, String msg) {
        return response(data, ErrorCodeEnum.SUCCESS.getErrorCode(), msg);
    }


    public static <T> ResponseResult<T> fail() {
        return response(null, ErrorCodeEnum.FAIL);
    }

    public static <T> ResponseResult<T> fail(String msg) {
        return response(null, ErrorCodeEnum.FAIL.getErrorCode(), msg);
    }

    public static <T> ResponseResult<T> fail(Integer code, String msg) {
        return response(null, code, msg);
    }

    public static <T> ResponseResult<T> securityFail(Integer code, String msg) {
        ResponseResult<T> apiResult = new ResponseResult<>();
        apiResult.setCode(code);
        apiResult.setMsg(msg);
        return apiResult;
    }

    public static <T> ResponseResult<T> fail(ErrorCodeEnum errorCodeEnum) {
        return response(errorCodeEnum.getErrorCode(),errorCodeEnum.getErrorDesc());
    }

    public static <T> ResponseResult<T> failData(Object data4Fail) {
        ResponseResult<T> result = new ResponseResult<>();
        result.setCode(ErrorCodeEnum.FAIL_DATA.getErrorCode());
        result.setMsg(ErrorCodeEnum.FAIL_DATA.getErrorDesc());
        result.data4Fail = data4Fail;
        return result;
    }

    public static <T> ResponseResult<T>  failData(Object data4Fail, String msg) {
        ResponseResult<T> result = new ResponseResult<>();
        result.setCode(ErrorCodeEnum.FAIL_DATA.getErrorCode());
        result.setMsg(msg);
        result.data4Fail = data4Fail;
        return result;
    }
}
