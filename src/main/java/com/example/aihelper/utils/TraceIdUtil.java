package com.example.aihelper.utils;

import org.slf4j.MDC;

import com.example.aihelper.logging.TraceIdFilter;

/**
 * TraceId 访问工具。
 */
public final class TraceIdUtil {

    private TraceIdUtil() {
    }

    /**
     * 获取当前请求上下文中的 TraceId。
     */
    public static String getTraceId() {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        return traceId == null ? "N/A" : traceId;
    }
}
