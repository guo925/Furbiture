package com.gjx.common;

import lombok.Data;

/**
 * 通用响应类
 * @param <T> 响应数据类型
 */
@Data
public class R<T> {
    /**
     * 响应码
     */
    private Integer code;
    
    /**
     * 响应消息
     */
    private String msg;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 私有构造方法
     */
    private R() {}
    
    /**
     * 成功响应
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 响应对象
     */
    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(ResultCode.SUCCESS.getCode());
        r.setMsg(ResultCode.SUCCESS.getMsg());
        r.setData(data);
        return r;
    }
    
    /**
     * 成功响应（无数据）
     * @param <T> 数据类型
     * @return 响应对象
     */
    public static <T> R<T> ok() {
        return ok(null);
    }
    
    /**
     * 错误响应
     * @param msg 错误消息
     * @param <T> 数据类型
     * @return 响应对象
     */
    public static <T> R<T> error(String msg) {
        R<T> r = new R<>();
        r.setCode(ResultCode.ERROR.getCode());
        r.setMsg(msg);
        return r;
    }
    
    /**
     * 错误响应
     * @param resultCode 响应码枚举
     * @param <T> 数据类型
     * @return 响应对象
     */
    public static <T> R<T> error(ResultCode resultCode) {
        R<T> r = new R<>();
        r.setCode(resultCode.getCode());
        r.setMsg(resultCode.getMsg());
        return r;
    }
    
    /**
     * 错误响应
     * @param resultCode 响应码枚举
     * @param msg 错误消息
     * @param <T> 数据类型
     * @return 响应对象
     */
    public static <T> R<T> error(ResultCode resultCode, String msg) {
        R<T> r = new R<>();
        r.setCode(resultCode.getCode());
        r.setMsg(msg);
        return r;
    }
}
