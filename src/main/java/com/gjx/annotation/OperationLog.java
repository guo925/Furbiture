package com.gjx.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * <p>
 * 标记在 Controller 方法上，由 AOP 切面自动记录操作日志。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {
    /** 操作类型：CREATE/UPDATE/DELETE/LOGIN 等 */
    String action();
    /** 操作目标描述（支持 SpEL，如 "'商品:' + #productId"）*/
    String target() default "";
}
