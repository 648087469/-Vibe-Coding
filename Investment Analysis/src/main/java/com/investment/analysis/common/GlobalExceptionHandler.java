package com.investment.analysis.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：统一返回 ApiResponse，避免前端拿到 500 页面。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数校验失败：{}", e.getMessage());
        return ApiResponse.fail(400, e.getMessage());
    }

    @ExceptionHandler(IndexDataException.class)
    public ApiResponse<Void> handleIndexData(IndexDataException e) {
        log.error("指数数据处理失败：{}", e.getMessage(), e);
        return ApiResponse.fail(500, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleOthers(Exception e) {
        log.error("系统异常", e);
        return ApiResponse.fail(500, "系统异常：" + e.getMessage());
    }
}
