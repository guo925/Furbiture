package com.gjx.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求链路追踪过滤器
 * <p>
 * 为每个请求生成（或沿用上游传入的）traceId，写入 SLF4J MDC 并回写到响应头。
 * 这样一条请求产生的所有日志都能通过 traceId 串起来，生产排障时可直接检索定位，
 * 而不必在多线程交错的日志里靠时间戳猜测。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    /**
     * 日志 MDC 中的键名，与日志格式 pattern 中的 %X{traceId} 对应
     */
    public static final String TRACE_ID_MDC_KEY = "traceId";

    /**
     * 用于跨服务传递 traceId 的请求/响应头（网关或上游已生成时直接沿用，实现全链路统一）
     */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        MDC.put(TRACE_ID_MDC_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 必须清理：Tomcat 线程会被复用，残留的 MDC 会污染后续请求的日志
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }
}
