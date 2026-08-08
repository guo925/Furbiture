package com.gjx.aspect;

import com.gjx.annotation.OperationLog;
import com.gjx.mapper.OperationLogMapper;
import com.gjx.util.AuthenticationUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 操作日志 AOP 切面
 * <p>
 * 拦截所有标注 @OperationLog 的方法，自动记录操作日志。
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog opLog) throws Throwable {
        Object result = joinPoint.proceed();

        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return result;

            HttpServletRequest request = attrs.getRequest();
            com.gjx.entity.OperationLog entity = new com.gjx.entity.OperationLog();
            entity.setAction(opLog.action());
            entity.setTarget(opLog.target());
            entity.setIp(request.getRemoteAddr());

            try {
                Long userId = AuthenticationUtil.getUserIdFromRequest(request);
                entity.setUserId(userId);
                entity.setUsername(AuthenticationUtil.getUsernameFromRequest(request));
            } catch (Exception ignored) {
                // 未登录操作不记录 userId
            }

            operationLogMapper.insert(entity);
        } catch (Exception e) {
            log.warn("记录操作日志失败: {}", e.getMessage());
        }

        return result;
    }
}
