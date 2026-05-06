package com.gjx.common;

/**
 * 业务异常类
 * 用于处理业务逻辑中的异常情况
 */
public class BusinessException extends RuntimeException {
    /**
     * 响应码
     */
    private final ResultCode resultCode;
    
    /**
     * 构造方法
     * @param message 异常消息
     */
    public BusinessException(String message) {
        super(message);
        resultCode = ResultCode.BUSINESS_ERROR;
    }
    
    /**
     * 构造方法
     * @param resultCode 响应码枚举
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.resultCode = resultCode;
    }
    
    /**
     * 构造方法
     * @param resultCode 响应码枚举
     * @param message 异常消息
     */
    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }
    
    /**
     * 获取响应码
     * @return 响应码枚举
     */
    public ResultCode getResultCode() {
        return resultCode;
    }
}
