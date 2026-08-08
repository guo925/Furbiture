package com.gjx.exception;

import com.gjx.common.BusinessException;
import com.gjx.common.R;
import com.gjx.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理应用中所有未捕获的异常，确保返回格式一致的错误响应。
 *
 * <p>职责：
 * <ul>
 *   <li>捕获 BusinessException，返回对应的业务错误码和消息</li>
 *   <li>捕获参数校验异常（@Valid），返回字段级错误详情</li>
 *   <li>捕获权限不足异常，返回 403</li>
 *   <li>兜底捕获未知异常，记录日志并返回 500</li>
 * </ul>
 *
 * <p>关键改进：使用 ResponseEntity 设置正确的 HTTP 状态码，
 * 而非始终返回 HTTP 200（浏览器 Network 面板可直观区分成功/失败）。
 *
 * @author Furbiture Team
 * @since 2026-08-08
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     * <p>
     * 业务层通过 throw new BusinessException(...) 主动抛出的异常在此统一处理。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<R<Void>> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("[业务异常] path={}, code={}, message={}",
                request.getRequestURI(), e.getResultCode().getCode(), e.getMessage());
        HttpStatus httpStatus = mapResultCodeToHttpStatus(e.getResultCode());
        return ResponseEntity.status(httpStatus).body(R.error(e.getResultCode(), e.getMessage()));
    }

    /**
     * 处理 @Valid 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<List<ErrorResponse.ValidationError>>> handleValidationException(
            MethodArgumentNotValidException e, HttpServletRequest request) {

        List<ErrorResponse.ValidationError> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> new ErrorResponse.ValidationError(
                        fieldError.getField(),
                        fieldError.getDefaultMessage()))
                .collect(Collectors.toList());

        log.warn("[参数校验失败] path={}, errors={}", request.getRequestURI(), errors);
        String message = errors.isEmpty() ? "参数校验失败" : errors.get(0).getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(R.error(ResultCode.PARAM_ERROR, message));
    }

    /**
     * 处理表单绑定校验异常（用于 GET 请求参数校验）
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<R<List<ErrorResponse.ValidationError>>> handleBindException(
            BindException e, HttpServletRequest request) {

        List<ErrorResponse.ValidationError> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> new ErrorResponse.ValidationError(
                        fieldError.getField(),
                        fieldError.getDefaultMessage()))
                .collect(Collectors.toList());

        log.warn("[参数绑定失败] path={}, errors={}", request.getRequestURI(), errors);
        String message = errors.isEmpty() ? "参数错误" : errors.get(0).getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(R.error(ResultCode.PARAM_ERROR, message));
    }

    /**
     * 处理权限不足异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<R<Void>> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.warn("[权限不足] path={}, message={}", request.getRequestURI(), e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(R.error(ResultCode.FORBIDDEN, "权限不足，无法访问该资源"));
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<R<Void>> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("[参数非法] path={}, message={}", request.getRequestURI(), e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(R.error(ResultCode.PARAM_ERROR, e.getMessage()));
    }

    /**
     * 兜底处理所有未预期的异常
     * <p>
     * 不应被上层针对性捕获的任何异常最终会到这里。
     * 记录完整的堆栈日志用于排查，但只向前端返回通用的错误消息，避免泄露内部细节。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleUnknownException(Exception e, HttpServletRequest request) {
        log.error("[系统异常] path={}, type={}, message={}",
                request.getRequestURI(), e.getClass().getName(), e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.error(ResultCode.ERROR, "服务器内部错误，请联系管理员"));
    }

    /**
     * 将内部 ResultCode 映射为 HTTP 状态码
     */
    private HttpStatus mapResultCodeToHttpStatus(ResultCode resultCode) {
        return switch (resultCode) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case PARAM_ERROR -> HttpStatus.BAD_REQUEST;
            case BUSINESS_ERROR -> HttpStatus.CONFLICT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
