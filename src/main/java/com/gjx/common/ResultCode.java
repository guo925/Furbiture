package com.gjx.common;

/**
 * 响应码枚举类
 * 遵循HTTP状态码规范
 */
public enum ResultCode {
    /**
     * 成功
     */
    SUCCESS(200, "成功"),
    
    /**
     * 服务器内部错误
     */
    ERROR(500, "服务器内部错误"),
    
    /**
     * 未授权
     */
    UNAUTHORIZED(401, "未授权"),
    
    /**
     * 禁止访问
     */
    FORBIDDEN(403, "禁止访问"),
    
    /**
     * 资源不存在
     */
    NOT_FOUND(404, "资源不存在"),
    
    /**
     * 参数错误
     */
    PARAM_ERROR(400, "参数错误"),
    
    /**
     * 业务错误
     */
    BUSINESS_ERROR(409, "业务错误");
    
    private final int code;
    private final String msg;
    
    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getMsg() {
        return msg;
    }
}
