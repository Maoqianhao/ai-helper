package com.example.aihelper.controller;

import com.example.aihelper.dto.ApiErrorResponse;
import com.example.aihelper.utils.TraceIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        String traceId = TraceIdUtil.getTraceId();
        log.warn("[traceId={}] [http-error] type=illegal_argument message={}", traceId, ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of("INVALID_ARGUMENT", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnhandledException(Exception ex) {
        String traceId = TraceIdUtil.getTraceId();
        log.error("[traceId={}] [http-error] type=unhandled_exception message={}", traceId, ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of("INTERNAL_SERVER_ERROR", "服务暂时不可用，请稍后再试"));
    }
}
