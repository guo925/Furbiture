package com.gjx.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 统一错误响应体
 * 用于全局异常处理器返回标准化的错误信息
 *
 * @author Furbiture Team
 * @since 2026-08-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * HTTP 状态码
     */
    private Integer code;

    /**
     * 错误消息
     */
    private String msg;

    /**
     * 错误发生时间
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * 请求路径
     */
    private String path;

    /**
     * 参数校验错误详情（仅校验失败时返回）
     */
    private List<ValidationError> errors;

    /**
     * 参数校验错误项
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ValidationError {
        /**
         * 校验失败的字段名
         */
        private String field;

        /**
         * 校验失败的原因
         */
        private String message;
    }
}
